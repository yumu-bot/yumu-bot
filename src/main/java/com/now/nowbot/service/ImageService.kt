package com.now.nowbot.service

import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.json.BeatMap
import com.now.nowbot.model.json.LazerScore
import com.now.nowbot.model.json.OsuUser
import com.now.nowbot.model.ppminus.PPMinus
import com.now.nowbot.throwable.GeneralTipsException
import io.netty.handler.timeout.ReadTimeoutException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientRequestException
import org.springframework.web.reactive.function.client.WebClientResponseException.BadRequest
import org.springframework.web.reactive.function.client.WebClientResponseException.InternalServerError
import java.net.ConnectException

@Service("NOWBOT_IMAGE")
class ImageService(private val webClient: WebClient) {
    /**
     * @param name 面板的内部编号，并非功能编号
     */
    fun getPanel(body: Map<String, Any>, name: String): ByteArray {
        val headers = defaultHeader
        val httpEntity = HttpEntity(body, headers)
        return doPost("panel_$name", httpEntity)
    }

    /**
     * @param name 面板的内部编号，并非功能编号
     */
    fun getPanel(any: Any?, name: String): ByteArray {
        val headers = defaultHeader
        val httpEntity = HttpEntity(any, headers)
        return doPost("panel_$name", httpEntity)
    }

    /**
     * 获取 md 图片，现已经弃用，被 panel A6 代替
     *
     * @param markdown md 字符串
     * @return 图片流
     */
    @Deprecated("") fun getMarkdownImage(markdown: String): ByteArray {
        val headers = defaultHeader

        val body: Map<String, Any> = mapOf("md" to markdown, "width" to 1500)
        val httpEntity = HttpEntity<Map<String, Any>>(body, headers)
        return doPost("md", httpEntity)
    }

    /***
     * 获取 md 图片，现已经弃用，被 panel A6 代替
     * 宽度是px,最好600以上
     * @param markdown md 字符串
     * @param width 宽度
     * @return 图片流
     */
    @Deprecated("") fun getMarkdownImage(markdown: String, width: Int): ByteArray {
        val headers = defaultHeader

        val body: Map<String, Any> = mapOf("md" to markdown, "width" to width)
        val httpEntity = HttpEntity<Map<String, Any>>(body, headers)
        return doPost("md", httpEntity)
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
        val headers = defaultHeader

        val body = mutableMapOf<String, Any>(
            "markdown" to markdown, "name" to name, "width" to width
        )

        if (user != null) {
            body["user"] = user
        }

        val httpEntity = HttpEntity<Map<String, Any>>(body, headers)
        return doPost("panel_A6", httpEntity)
    }

    fun getPanelAlpha(vararg lines: String?): ByteArray {
        val headers = defaultHeader
        val body: MutableMap<String, Any> = HashMap()
        body["strs"] = lines
        val httpEntity = HttpEntity<Map<String, Any>>(body, headers)
        return doPost("panel_Alpha", httpEntity)
    }

    /*
    fun getPanelBeta(s: LazerScore?): ByteArray {
        val headers = defaultHeader
        val httpEntity = HttpEntity(s, headers)
        return doPost("panel_Beta", httpEntity)
    }

     */

    fun getPanelGamma(score: LazerScore): ByteArray {
        val headers = defaultHeader
        val body = mapOf(
            "score" to score,
            "panel" to "score"
        )
        val httpEntity = HttpEntity<Map<String, Any>>(body, headers)
        return doPost("panel_Gamma", httpEntity)
    }

    fun getPanelGamma(osuUser: OsuUser): ByteArray {
        val headers = defaultHeader
        val body: MutableMap<String, Any> = HashMap()
        body["user"] = osuUser
        body["panel"] = "info"
        val httpEntity = HttpEntity<Map<String, Any>>(body, headers)
        return doPost("panel_Gamma", httpEntity)
    }

    fun getPanelGamma(user: OsuUser?, mode: OsuMode, my: PPMinus?): ByteArray {
        val cardA1 = if (user == null) emptyList() else listOf(user)

        val cardB = if (my == null) emptyMap() else mapOf(
            "ACC" to my.value1,
            "PTT" to my.value2,
            "STA" to my.value3,
            (if (mode == OsuMode.MANIA) "PRE" else "STB") to my.value4,
            "EFT" to my.value5,
            "STH" to my.value6,
            "OVA" to my.value7,
            "SAN" to my.value8
        )

        val statistics: Map<String, Any> = mapOf("is_vs" to false, "mode_int" to mode.modeValue)

        val headers = defaultHeader

        val body = mapOf(
            "users" to cardA1, "my" to cardB, "stat" to statistics, "panel" to "sanity"
        )

        val httpEntity = HttpEntity<Map<String, Any?>>(body, headers)
        return doPost("panel_Gamma", httpEntity)
    }

    fun getPanelDelta(beatMap: BeatMap, round: String, mod: String, position: Short, hasBG: Boolean): ByteArray {
        val headers = defaultHeader
        val body = mapOf(
            "beatmap" to beatMap, "round" to round, "mod" to mod, "position" to position, "hasBG" to hasBG
        )
        val httpEntity = HttpEntity<Map<String, Any>>(body, headers)
        return doPost("panel_Delta", httpEntity)
    }

    fun getPanelAlpha(sb: StringBuilder): ByteArray {
        return getPanelAlpha(*sb.toString().split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
    }

    private val defaultHeader: HttpHeaders
        get() {
            val headers = HttpHeaders()
            headers.contentType = MediaType.APPLICATION_JSON
            return headers
        }

    @Throws(GeneralTipsException::class) private fun doPost(path: String, entity: HttpEntity<*>): ByteArray {
        val request = webClient.post().uri(IMAGE_PATH + path).headers { it.addAll(entity.headers) }
        if (entity.hasBody()) {
            request.bodyValue(entity.body!!)
        }

        // 在这里封好可能出现的（已知原因的）错误，确保错误不会传递下去
        return try {
            request.retrieve().bodyToMono(ByteArray::class.java).block()!!
        } catch (e: Throwable) {
            if (e is BadRequest || e.cause is BadRequest) {
                throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Render_400)
            } else if (e is ReadTimeoutException || e.cause is ReadTimeoutException) {
                throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Render_408)
            } else if (e is InternalServerError || e.cause is InternalServerError) {
                throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Render_500)
            } else if (e is ConnectException || e.cause is ConnectException) {
                throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Render_503)
            } else if (e is WebClientRequestException) {
                throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Render_502)
            } else if (e is GeneralTipsException) {
                throw e
            } else {
                log.error("渲染模块：未识别的错误", e)
                throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Render_000)
            }
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(ImageService::class.java)
        const val IMAGE_PATH: String = "http://127.0.0.1:1611/"
    }
}
