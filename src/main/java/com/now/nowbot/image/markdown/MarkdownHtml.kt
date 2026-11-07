package com.now.nowbot.image.markdown

import com.now.nowbot.util.JacksonUtil

internal object MarkdownHtml {
    private const val SCRIPT_TAG =
        """<script type="module" crossorigin src="./assets/index.29dac38f.js"></script>"""
    private const val LINK_TAG =
        """<link rel="stylesheet" href="./assets/index.3bc2ed74.css">"""
    private const val WHITE_STYLE =
        "<style>html, body { background: #FFFFFF !important; }</style>"
    private val FONT_URL = Regex("""url\((\.\./\.\./\.\./font/[^)]+)\)""")

    fun build(
        markdown: String,
        width: Int,
        height: Int,
        fontUrl: (String) -> String,
    ): String {
        val shell = MarkdownAssets.shell()
        check(shell.contains(SCRIPT_TAG)) { "Card_Alpha 模板缺少脚本入口" }
        check(shell.contains(LINK_TAG)) { "Card_Alpha 模板缺少样式入口" }

        val style = renderStyle(MarkdownAssets.style(), fontUrl)
        val page = shell
            .replace(SCRIPT_TAG, "<script type=\"module\">${MarkdownAssets.bundle()}</script>")
            .replace(LINK_TAG, "<style>$style</style>")
            .replace("</head>", "$WHITE_STYLE</head>")
            .replace("</body>", "${bootstrap(markdown, width, height)}</body>")
        return page
    }

    private fun renderStyle(style: String, fontUrl: (String) -> String): String {
        val rendered = FONT_URL.replace(style) { match ->
            val name = match.groupValues[1].substringAfterLast('/')
            "url(\"${fontUrl(name)}\")"
        }
        check(!rendered.contains("../../../font/")) { "存在未替换的 Markdown 字体 URL" }
        return rendered
    }

    private fun bootstrap(markdown: String, width: Int, height: Int): String {
        val json = JacksonUtil.mapper.writeValueAsString(markdown)
            .replace("</", "<\\/")
            .replace("\u2028", "\\u2028")
            .replace("\u2029", "\\u2029")
        return """
<script>
(function () {
  var markdown = $json;
  var fallbackWidth = $width;
  var fallbackHeight = $height;

  function clamp(value, fallback) {
    var number = Number(value);
    if (!isFinite(number) || number <= 0) return fallback;
    return Math.max(1, Math.min(16384, Math.round(number)));
  }

  function waitFor(check, timeout, message) {
    return new Promise(function (resolve, reject) {
      var deadline = Date.now() + timeout;
      var timer = setInterval(function () {
        var ok = false;
        try { ok = check(); }
        catch (error) { clearInterval(timer); reject(error); return; }
        if (ok) {
          clearInterval(timer);
          resolve();
          return;
        }
        if (Date.now() > deadline) {
          clearInterval(timer);
          reject(new Error(message));
        }
      }, 10);
    });
  }

  window.__RENDER_READY__ = (async function () {
    await waitFor(function () {
      return typeof window.setStr === 'function';
    }, 15000, 'Markdown setStr 超时');
    window.setStr(markdown);

    await waitFor(function () {
      return !!document.querySelector('article');
    }, 15000, 'Markdown article 渲染超时');

    if (document.fonts && document.fonts.ready) {
      await document.fonts.ready;
    }

    var images = Array.from(document.images);
    await Promise.all(images.map(function (image) {
      if (image.complete) return Promise.resolve();
      return new Promise(function (resolve) {
        image.addEventListener('load', resolve, { once: true });
        image.addEventListener('error', resolve, { once: true });
      });
    }));

    await new Promise(function (resolve) {
      requestAnimationFrame(function () {
        requestAnimationFrame(resolve);
      });
    });

    var rect = document.body.getBoundingClientRect();
    var boundedWidth = clamp(rect.width, fallbackWidth);
    var boundedHeight = clamp(rect.height, fallbackHeight);

    function applyHeight(element) {
      if (!element) return;
      element.style.setProperty('height', boundedHeight + 'px', 'important');
      element.style.setProperty('max-height', boundedHeight + 'px', 'important');
      element.style.setProperty('min-height', '0', 'important');
      element.style.setProperty('overflow', 'hidden', 'important');
    }

    applyHeight(document.documentElement);
    applyHeight(document.body);
    document.querySelectorAll('#app, #root, article, main').forEach(applyHeight);

    return { width: boundedWidth, height: boundedHeight };
  })();
})();
</script>
"""
    }
}
