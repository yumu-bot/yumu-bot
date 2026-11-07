package com.now.nowbot.browser

import com.microsoft.playwright.*
import com.microsoft.playwright.options.LoadState
import com.microsoft.playwright.options.ScreenshotType
import com.microsoft.playwright.options.WaitUntilState
import com.now.nowbot.config.BrowserConfig
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.concurrent.*
import java.util.function.Predicate

internal class RenderSlot(
    private val slotIndex: Int,
    private val config: BrowserConfig,
) {
    val executor: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "html-renderer-slot-$slotIndex").apply { isDaemon = true }
    }

    private var playwright: Playwright? = null
    private var browser: Browser? = null
    private var context: BrowserContext? = null
    private var page: Page? = null

    fun execute(html: String, options: ScreenshotOptions): ByteArray {
        val deadline = RenderDeadline(config.renderTimeoutMillis)
        val activePage = ensurePage(deadline)
        var diagnostics: PageDiagnostics? = null
        var result: ByteArray? = null
        var failure: Throwable? = null

        try {
            val task = PageDiagnostics(activePage)
            diagnostics = task
            task.install()

            activePage.setViewportSize(options.width, options.height)
            activePage.setContent(
                html,
                Page.SetContentOptions()
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                    .setTimeout(deadline.timeout("加载 HTML")),
            )
            try {
                activePage.waitForLoadState(
                    LoadState.LOAD,
                    Page.WaitForLoadStateOptions().setTimeout(deadline.timeout("等待 load 事件")),
                )
            } catch (e: PlaywrightException) {
                throw RenderException("等待 load 事件失败: ${e.message}", e)
            }
            awaitReady(activePage, deadline)
            task.requireHealthy()
            result = capture(activePage, options, deadline)
            task.requireHealthy()
        } catch (t: Throwable) {
            failure = t
        } finally {
            val cleanupOk = diagnostics?.let { task ->
                runCatching { task.close() }.getOrDefault(false)
            } ?: true
            if (!cleanupOk) {
                resetPage()
            }
        }

        val cause = failure
        if (cause != null) {
            val issueSummary = diagnostics?.summary().orEmpty()
            resetPage()
            if (cause is Error) {
                throw cause
            }
            throw renderFailure(cause, issueSummary)
        }

        return result ?: throw RenderException("渲染未产生截图")
    }

    fun close(timeoutMillis: Long): Boolean {
        var interrupted = false
        var clean = true
        val deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis)
        val cleanup = try {
            executor.submit(Callable { closeResources() })
        } catch (_: RejectedExecutionException) {
            null
        }
        if (cleanup == null) {
            clean = false
        } else {
            while (true) {
                val remaining = deadlineNanos - System.nanoTime()
                if (remaining <= 0L) {
                    executor.shutdownNow()
                    clean = false
                    break
                }
                try {
                    if (!cleanup.get(remaining, TimeUnit.NANOSECONDS)) {
                        clean = false
                    }
                    break
                } catch (_: InterruptedException) {
                    interrupted = true
                } catch (_: ExecutionException) {
                    clean = false
                    break
                } catch (_: TimeoutException) {
                    executor.shutdownNow()
                    clean = false
                    break
                }
            }
        }
        executor.shutdown()
        val remaining = deadlineNanos - System.nanoTime()
        if (remaining > 0L) {
            try {
                if (!executor.awaitTermination(remaining, TimeUnit.NANOSECONDS)) {
                    executor.shutdownNow()
                    clean = false
                }
            } catch (_: InterruptedException) {
                interrupted = true
                executor.shutdownNow()
                clean = false
            }
        } else if (!executor.isTerminated) {
            executor.shutdownNow()
            clean = false
        }
        if (interrupted) {
            Thread.currentThread().interrupt()
        }
        return clean
    }

    private fun awaitReady(page: Page, deadline: RenderDeadline) {
        val timeoutMillis = deadline.timeout("等待页面就绪")
        try {
            page.evaluate(READY_SCRIPT, timeoutMillis)
        } catch (e: PlaywrightException) {
            throw RenderException("等待页面就绪失败: ${e.message}", e)
        }
    }

    private fun capture(page: Page, options: ScreenshotOptions, deadline: RenderDeadline): ByteArray {
        val screenshotOptions = Page.ScreenshotOptions()
            .setFullPage(options.fullPage)
            .setOmitBackground(options.omitBackground)
            .setType(options.format)
            .setTimeout(deadline.timeout("截图"))

        if (options.format == ScreenshotType.JPEG || options.format == ScreenshotType.WEBP) {
            screenshotOptions.setQuality(options.jpegQuality)
        }
        return try {
            page.screenshot(screenshotOptions)
        } catch (e: PlaywrightException) {
            throw RenderException("截图失败: ${e.message}", e)
        }
    }

    private fun ensurePage(deadline: RenderDeadline): Page {
        try {
            if (playwright == null || browser?.isConnected() != true) {
                closeResources()
                playwright = Playwright.create(
                    Playwright.CreateOptions().setEnv(
                        HashMap(System.getenv()).apply {
                            put("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")
                        },
                    ),
                )
                browser = launchBrowser(playwright!!, deadline)
            }
            if (context == null || context!!.isClosed()) {
                context = browser!!.newContext(
                    Browser.NewContextOptions()
                        .setDeviceScaleFactor(config.deviceScaleFactor)
                        .setLocale(config.locale)
                        .setTimezoneId(config.timezone),
                )
            }
            val current = page
            if (current == null || current.isClosed()) {
                page = context!!.newPage()
            }
            return page!!
        } catch (e: Exception) {
            closeResources()
            if (e is RenderException) {
                throw e
            }
            throw RenderException("初始化浏览器失败: ${e.message}", e)
        }
    }

    private fun launchBrowser(playwright: Playwright, deadline: RenderDeadline): Browser {
        val remaining = deadline.remainingMillis()
        if (remaining <= 0L) {
            throw RenderException("渲染超时: 启动浏览器")
        }
        val launchTimeout = minOf(config.launchTimeoutMillis, remaining)
        val launchOptions = BrowserType.LaunchOptions()
            .setHeadless(true)
            .setChromiumSandbox(config.chromiumSandbox)
            .setTimeout(launchTimeout.toDouble())
        if (config.executablePath.isNotBlank()) {
            val executable = Path.of(config.executablePath)
            if (!Files.isRegularFile(executable)) {
                throw RenderException("浏览器不存在: $executable")
            }
            if (!Files.isExecutable(executable)) {
                throw RenderException("浏览器不可执行: $executable")
            }
            launchOptions.setExecutablePath(executable)
        }
        return playwright.chromium().launch(launchOptions)
    }

    private fun resetPage() {
        var clean = true
        clean = runCatching { page?.close() }.also { page = null }.isSuccess && clean
        clean = runCatching { context?.close() }.also { context = null }.isSuccess && clean
        if (!clean || browser?.isConnected() != true) {
            closeResources()
        }
    }

    private fun closeResources(): Boolean {
        var clean = true
        clean = runCatching { page?.close() }.isSuccess && clean
        page = null
        clean = runCatching { context?.close() }.isSuccess && clean
        context = null
        clean = runCatching { browser?.close() }.isSuccess && clean
        browser = null
        clean = runCatching { playwright?.close() }.isSuccess && clean
        playwright = null
        return clean
    }

    private fun renderFailure(cause: Throwable, issueSummary: String): RenderException {
        val message = cause.message ?: cause::class.simpleName ?: "渲染失败"
        if (issueSummary.isBlank()) {
            return if (cause is RenderException) cause else RenderException(message, cause)
        }
        return RenderException(message + "\n" + issueSummary, cause)
    }
}

private class RenderDeadline(totalMillis: Long) {
    private val deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(totalMillis)

    fun remainingMillis(): Long =
        TimeUnit.NANOSECONDS.toMillis(deadlineNanos - System.nanoTime()).coerceAtLeast(0L)

    fun timeout(stage: String): Double {
        val remaining = remainingMillis()
        if (remaining <= 0L) {
            throw RenderException("渲染超时: $stage")
        }
        return remaining.toDouble()
    }
}

private class PageDiagnostics(private val page: Page) {
    private val context = page.context()
    private val pageErrors = CopyOnWriteArrayList<String>()
    private val consoleErrors = CopyOnWriteArrayList<String>()
    private val requestFailures = CopyOnWriteArrayList<String>()
    private val badResponses = CopyOnWriteArrayList<String>()
    private val blockedRequests = CopyOnWriteArrayList<String>()
    private val crashes = CopyOnWriteArrayList<String>()

    private val pageErrorHandler = java.util.function.Consumer<String> { pageErrors += it }
    private val consoleHandler = java.util.function.Consumer<ConsoleMessage> { message ->
        if (message.type() == "error") {
            consoleErrors += message.text() + " (" + message.location() + ")"
        }
    }
    private val requestFailedHandler = java.util.function.Consumer<Request> { request ->
        requestFailures += request.url() + " " + (request.failure() ?: "请求失败")
    }
    private val responseHandler = java.util.function.Consumer<Response> { response ->
        if (response.status() >= 400) {
            badResponses += response.status().toString() + " " + response.url()
        }
    }
    private val crashHandler = java.util.function.Consumer<Page> { crashes += "页面崩溃" }
    private val routeHandler = java.util.function.Consumer<Route> { route ->
        val url = route.request().url()
        if (isAllowedScheme(url)) {
            route.resume()
        } else {
            blockedRequests += url
            route.abort()
        }
    }
    private val webSocketHandler = java.util.function.Consumer<WebSocketRoute> { socket ->
        blockedRequests += socket.url()
        socket.close()
    }

    fun install() {
        page.onPageError(pageErrorHandler)
        page.onConsoleMessage(consoleHandler)
        page.onRequestFailed(requestFailedHandler)
        page.onResponse(responseHandler)
        page.onCrash(crashHandler)
        context.route("**/*", routeHandler)
        context.routeWebSocket(Predicate<String> { true }, webSocketHandler)
    }

    fun requireHealthy() {
        if (issues().isNotEmpty()) {
            throw RenderException("页面存在脚本、控制台或资源错误")
        }
    }

    fun issues(): List<String> = buildList {
        if (crashes.isNotEmpty()) {
            add("页面崩溃")
        }
        addAll(pageErrors.take(4).map { "页面脚本错误: $it" })
        addAll(consoleErrors.take(4).map { "控制台错误: $it" })
        addAll(blockedRequests.take(4).map { "已阻断外部请求: $it" })
        addAll(requestFailures.take(4).map { "资源加载失败: $it" })
        addAll(badResponses.take(4).map { "HTTP 错误: $it" })
    }

    fun summary(): String {
        val issues = issues()
        return if (issues.isEmpty()) "" else "页面诊断:\n" + issues.joinToString("\n")
    }

    fun close(): Boolean {
        var clean = true
        clean = runCatching {
            page.navigate(
                "about:blank",
                Page.NavigateOptions()
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED),
            )
        }.isSuccess && clean
        clean = runCatching { context.unrouteAll() }.isSuccess && clean
        clean = runCatching { page.offPageError(pageErrorHandler) }.isSuccess && clean
        clean = runCatching { page.offConsoleMessage(consoleHandler) }.isSuccess && clean
        clean = runCatching { page.offRequestFailed(requestFailedHandler) }.isSuccess && clean
        clean = runCatching { page.offResponse(responseHandler) }.isSuccess && clean
        clean = runCatching { page.offCrash(crashHandler) }.isSuccess && clean
        clean = runCatching { context.clearCookies() }.isSuccess && clean
        return clean
    }

    private fun isAllowedScheme(url: String): Boolean {
        val separator = url.indexOf(':')
        if (separator <= 0) {
            return false
        }
        val scheme = url.substring(0, separator).lowercase(Locale.ROOT)
        return scheme in ALLOWED_RESOURCE_SCHEMES
    }
}

private const val READY_SCRIPT = """
async (timeoutMs) => {
  const deadline = Date.now() + Math.max(0, timeoutMs);
  const remaining = () => Math.max(0, deadline - Date.now());
  const withDeadline = (promise, label) => new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error('等待 ' + label + ' 超时')), remaining());
    Promise.resolve(promise).then(
      value => { clearTimeout(timer); resolve(value); },
      error => { clearTimeout(timer); reject(error); }
    );
  });

  if (window.__RENDER_READY__ != null) {
    await withDeadline(window.__RENDER_READY__, '__RENDER_READY__');
  }

  if (document.fonts) {
    await withDeadline(document.fonts.ready, 'document.fonts.ready');
    const failedFont = Array.from(document.fonts).find(face => face.status === 'error');
    if (failedFont) {
      throw new Error('字体加载失败: ' + (failedFont.family || '未知字体'));
    }
  }

  const images = Array.from(document.images);
  await Promise.all(images.map(async image => {
    if (!image.complete) {
      await withDeadline(new Promise((resolve, reject) => {
        image.addEventListener('load', resolve, { once: true });
        image.addEventListener('error', () => reject(new Error('图片加载失败: ' + (image.currentSrc || image.src))), { once: true });
      }), '图片加载');
    }
    if (!image.naturalWidth || !image.naturalHeight) {
      throw new Error('图片尺寸无效: ' + (image.currentSrc || image.src));
    }
    if (typeof image.decode === 'function') {
      await withDeadline(image.decode(), '图片解码');
    }
  }));

  const svgImageHrefs = new Set();
  Array.from(document.querySelectorAll('image')).forEach(node => {
    const raw = (node.href && node.href.baseVal) ||
      node.getAttribute('href') || node.getAttribute('xlink:href') || '';
    const href = raw.trim();
    if (href && !href.startsWith('#')) {
      svgImageHrefs.add(href);
    }
  });
  await Promise.all(Array.from(svgImageHrefs).map(async href => {
    const image = new Image();
    image.src = href;
    try {
      await withDeadline(image.decode(), 'SVG 图片解码');
    } catch (error) {
      throw new Error('SVG 图片解码失败: ' + href.slice(0, 96));
    }
  }));

  await withDeadline(
    new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve))),
    '布局刷新'
  );
  return true;
}
"""

private val ALLOWED_RESOURCE_SCHEMES = setOf("data", "blob", "about")
