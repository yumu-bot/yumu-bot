package com.now.nowbot.service

import com.now.nowbot.browser.RenderException
import com.now.nowbot.entity.IDUser
import com.now.nowbot.image.render.PanelRenderDispatcher
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.model.ppminus.PPMinus
import com.now.nowbot.throwable.botRuntimeException.NetworkException
import com.now.nowbot.util.command.REGEX_CRLF
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeoutException

@Service("NOWBOT_IMAGE")
class ImageService(
    private val dispatcher: PanelRenderDispatcher,
) {
    /**
     * @param name 面板的内部编号，并非功能编号
     */
    fun getPanel(body: Map<String, Any>, name: String): ByteArray {
        return dispatch(name) { dispatcher.render(name, body) }
    }

    /**
     * @param name 面板的内部编号，并非功能编号
     */
    fun getPanel(any: Any?, name: String): ByteArray {
        return dispatch(name) { dispatcher.render(name, any) }
    }

    /**
     * Markdown 页面，用于帮助和维基 MD/H/W，user 默认 Optional.empty，width 默认 1840， data 默认 ""
     */
    fun getPanelA6(markdown: String): ByteArray {
        return getPanelA6(null, markdown, "", 1840)
    }

    /**
     * Markdown 页面，用于帮助和维基 MD/H/W，user 默认 Optional.empty，width 默认 1840
     */
    fun getPanelA6(markdown: String, name: String): ByteArray {
        return getPanelA6(null, markdown, name, 1840)
    }

    /**
     * Markdown 页面，用于帮助和维基 MD/H/W， width 默认 1840， data 默认 null
     */
    fun getPanelA6(user: OsuUser?, markdown: String): ByteArray {
        return getPanelA6(user, markdown, "", 1840)
    }

    /**
     * Markdown 页面，用于帮助和维基 MD/H/W， width 默认 1840
     */
    fun getPanelA6(user: OsuUser?, markdown: String, name: String): ByteArray {
        return getPanelA6(user, markdown, name, 1840)
    }

    /**
     * Markdown 页面，用于帮助和维基 MD/H/W
     *
     * @param user     左上角的玩家，可以为 Optional.empty
     * @param markdown md 字符串
     * @param name     名字，仅支持 null、wiki、help
     * @param width    默认 1840
     * @return 图片流
     */
    fun getPanelA6(user: OsuUser?, markdown: String, name: String, width: Int = 1840): ByteArray {
        return dispatch("A6") { dispatcher.renderA6(user, markdown, name, width) }
    }

    fun getPanelAlpha(vararg lines: String?): ByteArray {
        return dispatch("Alpha") { dispatcher.renderAlpha(lines.toList()) }
    }

    fun getPanelGamma(score: LazerScore): ByteArray {
        return dispatch("Gamma") { dispatcher.renderGamma(score) }
    }

    fun getPanelGamma(user: IDUser): ByteArray {
        return dispatch("Gamma") { dispatcher.renderGamma(user) }
    }

    fun getPanelGamma(user: OsuUser?, mode: OsuMode, my: PPMinus?): ByteArray {
        return dispatch("Gamma") { dispatcher.renderGamma(user, mode, my) }
    }

    fun getPanelDelta(beatmap: Beatmap, round: String, mod: String, position: Short, hasBG: Boolean): ByteArray {
        return dispatch("Delta") { dispatcher.renderDelta(beatmap, round, mod, position, hasBG) }
    }

    fun getPanelAlpha(sb: StringBuilder): ByteArray {
        return getPanelAlpha(*sb.toString().split(REGEX_CRLF).dropLastWhile { it.isEmpty() }.toTypedArray())
    }

    private inline fun dispatch(panel: String, block: () -> ByteArray): ByteArray {
        val bytes = try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: NetworkException) {
            throw e
        } catch (e: Throwable) {
            throw mapRenderFailure(panel, e)
        }
        if (bytes.isEmpty()) {
            log.error("渲染模块：面板 {} 返回空数据", panel)
            throw NetworkException.RenderModuleException.NoContent()
        }
        return bytes
    }

    private fun mapRenderFailure(panel: String, error: Throwable): NetworkException.RenderModuleException {
        log.error("渲染模块：面板 {} 渲染失败", panel, error)
        return when (error) {
            is TimeoutException -> NetworkException.RenderModuleException.GatewayTimeout()
            is InterruptedException -> NetworkException.RenderModuleException.GatewayTimeout()
            is IllegalArgumentException,
            is IllegalStateException,
            is UnsupportedOperationException,
            -> NetworkException.RenderModuleException.BadRequest()

            is RenderException -> when {
                isTimeout(error) -> NetworkException.RenderModuleException.GatewayTimeout()
                error.cause is IllegalArgumentException || error.cause is IllegalStateException ->
                    NetworkException.RenderModuleException.BadRequest()

                else -> NetworkException.RenderModuleException.Undefined(error)
            }

            else -> NetworkException.RenderModuleException.Undefined(error)
        }
    }

    private fun isTimeout(error: Throwable): Boolean {
        var current: Throwable? = error
        repeat(6) {
            val cause = current ?: return false
            if (cause is TimeoutException || cause is InterruptedException) return true
            val message = cause.message
            if (message != null && (message.contains("超时") || message.contains("timeout", ignoreCase = true))) {
                return true
            }
            current = cause.cause
        }
        return false
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(ImageService::class.java)
    }
}
