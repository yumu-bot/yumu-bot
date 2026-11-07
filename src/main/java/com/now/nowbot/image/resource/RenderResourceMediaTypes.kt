package com.now.nowbot.image.resource

import org.springframework.http.MediaType
import java.nio.charset.StandardCharsets
import java.util.Locale

object RenderResourceMediaTypes {
    private val WEBP = MediaType.parseMediaType("image/webp")
    private val AVIF = MediaType.parseMediaType("image/avif")
    private val BMP = MediaType.parseMediaType("image/bmp")
    private val SVG = MediaType.parseMediaType("image/svg+xml")
    private val ICO = MediaType.parseMediaType("image/x-icon")

    private val LEGACY_FONT_SUBTYPES = setOf(
        "font-woff",
        "font-woff2",
        "vnd.ms-fontobject",
        "x-font-woff",
        "x-font-woff2",
        "x-font-ttf",
        "x-font-opentype",
        "x-font-truetype",
        "x-font-otf",
    )

    fun parse(value: String): MediaType {
        val parsed = try {
            MediaType.parseMediaType(value.trim())
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("无效的渲染资源类型: $value", e)
        }
        return normalize(parsed)
    }

    fun normalize(mediaType: MediaType): MediaType {
        require(mediaType.type != "*" && mediaType.subtype != "*") {
            "不支持通配的渲染资源类型: $mediaType"
        }
        val type = mediaType.type.lowercase(Locale.ROOT)
        val subtype = mediaType.subtype.lowercase(Locale.ROOT)
        val normalized = when {
            type == "image" && subtype == "jpg" -> MediaType.IMAGE_JPEG
            type == "image" && subtype == "svg" -> SVG
            type == "image" && subtype == "ico" -> ICO
            else -> MediaType(type, subtype)
        }
        require(isAllowed(normalized)) { "不支持的渲染资源类型: $mediaType" }
        return normalized
    }

    fun detect(bytes: ByteArray): MediaType? {
        if (isPng(bytes)) return MediaType.IMAGE_PNG
        if (isJpeg(bytes)) return MediaType.IMAGE_JPEG
        if (isGif(bytes)) return MediaType.IMAGE_GIF
        if (isWebp(bytes)) return WEBP
        if (isAvif(bytes)) return AVIF
        if (isBmp(bytes)) return BMP
        if (isIco(bytes)) return ICO
        if (looksLikeSvg(bytes)) return SVG
        return null
    }

    private fun isAllowed(mediaType: MediaType): Boolean {
        val type = mediaType.type.lowercase(Locale.ROOT)
        val subtype = mediaType.subtype.lowercase(Locale.ROOT)
        if (type == "image" || type == "font") {
            return subtype.isNotBlank() && subtype != "*"
        }
        return type == "application" && subtype in LEGACY_FONT_SUBTYPES
    }

    private fun isPng(bytes: ByteArray): Boolean =
        bytes.size >= 8 &&
            bytes[0] == 0x89.toByte() &&
            bytes[1] == 0x50.toByte() &&
            bytes[2] == 0x4E.toByte() &&
            bytes[3] == 0x47.toByte() &&
            bytes[4] == 0x0D.toByte() &&
            bytes[5] == 0x0A.toByte() &&
            bytes[6] == 0x1A.toByte() &&
            bytes[7] == 0x0A.toByte()

    private fun isJpeg(bytes: ByteArray): Boolean =
        bytes.size >= 3 &&
            bytes[0] == 0xFF.toByte() &&
            bytes[1] == 0xD8.toByte() &&
            bytes[2] == 0xFF.toByte()

    private fun isGif(bytes: ByteArray): Boolean =
        matchesAscii(bytes, 0, "GIF87a") || matchesAscii(bytes, 0, "GIF89a")

    private fun isWebp(bytes: ByteArray): Boolean =
        matchesAscii(bytes, 0, "RIFF") && matchesAscii(bytes, 8, "WEBP")

    private fun isAvif(bytes: ByteArray): Boolean =
        matchesAscii(bytes, 4, "ftyp") &&
            (matchesAscii(bytes, 8, "avif") || matchesAscii(bytes, 8, "avis"))

    private fun isBmp(bytes: ByteArray): Boolean =
        bytes.size >= 2 &&
            bytes[0] == 'B'.code.toByte() &&
            bytes[1] == 'M'.code.toByte()

    private fun isIco(bytes: ByteArray): Boolean =
        bytes.size >= 4 &&
            bytes[0] == 0.toByte() &&
            bytes[1] == 0.toByte() &&
            bytes[2] == 1.toByte() &&
            bytes[3] == 0.toByte()

    private fun looksLikeSvg(bytes: ByteArray): Boolean {
        if (bytes.isEmpty()) return false
        val size = minOf(bytes.size, 512)
        val head = String(bytes, 0, size, StandardCharsets.UTF_8)
            .trimStart('\uFEFF', ' ', '\t', '\r', '\n')
        return head.startsWith("<svg", ignoreCase = true) ||
            (head.startsWith("<?xml", ignoreCase = true) && head.contains("<svg", ignoreCase = true))
    }

    private fun matchesAscii(bytes: ByteArray, offset: Int, value: String): Boolean {
        if (offset < 0 || offset + value.length > bytes.size) return false
        for (index in value.indices) {
            if (bytes[offset + index] != value[index].code.toByte()) return false
        }
        return true
    }
}
