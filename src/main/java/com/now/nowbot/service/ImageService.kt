package com.now.nowbot.service

import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.json.BeatMap
import com.now.nowbot.model.json.LazerScore
import com.now.nowbot.model.json.OsuUser
import com.now.nowbot.model.ppminus.PPMinus
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClientException
import org.springframework.web.reactive.function.client.WebClient
import java.io.Serializable
import java.util.*

@Service("NOWBOT_IMAGE") class ImageService(private val webClient: WebClient) {
    // 2024+ 统一获取方法
    fun getPanel(body: Map<String, Any>, name: String): ByteArray {
        val headers = defaultHeader
        val httpEntity = HttpEntity(body, headers)
        return doPost("panel_$name", httpEntity)
    }

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

        val body: Map<String, Serializable> = java.util.Map.of("md", markdown, "width", 1500)
        val httpEntity = HttpEntity<Map<String, *>>(body, headers)
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

        val body: Map<String, Serializable> = java.util.Map.of("md", markdown, "width", width)
        val httpEntity = HttpEntity<Map<String, *>>(body, headers)
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

    fun getPanelA7(user: OsuUser, fixes: Map<String, Any>): ByteArray {
        val headers = defaultHeader
        val body: Map<String, Any> = mapOf(
            "user" to user, "scores" to fixes["scores"]!!, "pp" to fixes["pp"]!!
        )

        val httpEntity = HttpEntity(body, headers)
        return doPost("panel_A7", httpEntity)
    }

    fun getPanelB1(user: OsuUser, mode: OsuMode, my: PPMinus): ByteArray {
        val cardA1 = listOf(user)

        val cardB = mapOf(
            "ACC" to my.value1,
            "PTT" to my.value2,
            "STA" to my.value3,
            (if (mode == OsuMode.MANIA) "PRE" else "STB") to my.value4,
            "EFT" to my.value5,
            "STH" to my.value6,
            "OVA" to my.value7,
            "SAN" to my.value8
        )

        val headers = defaultHeader

        val body = java.util.Map.of(
            "users", cardA1, "me", cardB, "stat", java.util.Map.of("is_vs", false, "mode_int", mode.modeValue)
        )
        val httpEntity = HttpEntity(body, headers)
        return doPost("panel_B1", httpEntity)
    }

    fun getPanelB1(
        me: OsuUser?, other: OsuUser?, my: PPMinus, others: PPMinus?, mode: OsuMode
    ): ByteArray {
        val isVs = other != null && others != null

        //var Card_A = List.of(getPanelBUser(userMe), getPanelBUser(userOther));
        val cardA1s = ArrayList<OsuUser?>(2)
        cardA1s.add(me)

        if (isVs) cardA1s.add(other)

        val cardB1 = mapOf(
            "ACC" to my.value1,
            "PTT" to my.value2,
            "STA" to my.value3,
            (if (mode == OsuMode.MANIA) "PRE" else "STB") to my.value4,
            "EFT" to my.value5,
            "STH" to my.value6,
            "OVA" to my.value7,
            "SAN" to my.value8
        )
        val cardB2 = if (isVs) mapOf(
            "ACC" to others!!.value1,
            "PTT" to others.value2,
            "STA" to others.value3,
            (if (mode == OsuMode.MANIA) "PRE" else "STB") to others.value4,
            "EFT" to others.value5,
            "STH" to others.value6,
            "OVA" to others.value7,
            "SAN" to others.value8
        ) else null

        val statistics: Map<String, Serializable> = java.util.Map.of("is_vs", isVs, "mode_int", mode.modeValue)
        val headers = defaultHeader

        val body = HashMap<String, Any?>(4)

        body["users"] = cardA1s
        body["my"] = cardB1
        body.putIfAbsent("others", cardB2)
        body["stat"] = statistics

        val httpEntity = HttpEntity<Map<String, Any?>>(body, headers)
        return doPost("panel_B1", httpEntity)
    }

    fun getPanelB3(hashMap: MutableMap<String, Any>): ByteArray {
        hashMap["isVs"] = hashMap.containsKey("other")
        val headers = defaultHeader

        val httpEntity = HttpEntity<Map<String, Any>>(hashMap, headers)
        return doPost("panel_B3", httpEntity)
    }

    fun getPanelH(mapPool: Any, mode: OsuMode): ByteArray { // log.debug(JacksonUtil.objectToJsonPretty(mapPool));
        val headers = defaultHeader

        val body = java.util.Map.of(
            "pool", mapPool, "mode", mode.shortName
        )

        val httpEntity = HttpEntity(body, headers)
        return doPost("panel_H", httpEntity)
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

    @Throws(RestClientException::class) private fun doPost(path: String, entity: HttpEntity<*>): ByteArray {
        val request = webClient.post().uri(IMAGE_PATH + path).headers { h: HttpHeaders -> h.addAll(entity.headers) }
        if (entity.hasBody()) {
            request.bodyValue(entity.body!!)
        }
        return request.retrieve().bodyToMono(ByteArray::class.java)
            .doOnError { e: Throwable? -> log.error("post image error", e) }.block()!!
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(ImageService::class.java)
        const val IMAGE_PATH: String = "http://127.0.0.1:1611/"
    }
}
