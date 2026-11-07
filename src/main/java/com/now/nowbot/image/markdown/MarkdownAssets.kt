package com.now.nowbot.image.markdown

import java.util.concurrent.ConcurrentHashMap

internal object MarkdownAssets {
    private const val ROOT = "/templates/draw/Card_Alpha/"
    private const val FONT_ROOT = "/static/font/draw/"
    private val FONT_NAME = Regex("[A-Za-z0-9._-]+")

    private val shell by lazy { readText("${ROOT}index.html") }
    private val bundle by lazy { readText("${ROOT}assets/index.29dac38f.js") }
    private val style by lazy { readText("${ROOT}assets/index.3bc2ed74.css") }
    private val fonts = ConcurrentHashMap<String, ByteArray>()

    fun shell(): String = shell

    fun bundle(): String = bundle

    fun style(): String = style

    fun font(name: String): ByteArray {
        require(FONT_NAME.matches(name) && name != "." && name != ".." && !name.contains("..")) {
            "非法的字体文件名: $name"
        }
        return fonts.computeIfAbsent(name) { readBytes("$FONT_ROOT$it") }
    }

    private fun readText(path: String): String =
        readBytes(path).toString(Charsets.UTF_8)

    private fun readBytes(path: String): ByteArray {
        val stream = MarkdownAssets::class.java.getResourceAsStream(path)
            ?: throw IllegalStateException("Markdown 资源不存在: $path")
        return stream.use { it.readBytes() }
    }
}
