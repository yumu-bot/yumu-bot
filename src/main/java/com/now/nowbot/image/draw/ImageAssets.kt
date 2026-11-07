package com.now.nowbot.image.draw

import com.now.nowbot.config.NowbotConfig
import com.now.nowbot.image.svg.Svg
import java.io.IOException
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.util.Locale
import java.util.concurrent.ThreadLocalRandom

fun interface RenderAssetRegistry {
    fun registerImage(bytes: ByteArray): String
}

fun interface RemoteImageSource {
    fun read(url: String): ByteArray?
}

class RenderReport {
    private val missingAssets = linkedSetOf<String>()

    val missing: Set<String> get() = missingAssets

    fun missing(name: String) {
        missingAssets += name
    }
}

class ImageAssets(
    private val registry: RenderAssetRegistry,
    private val remote: RemoteImageSource? = null,
    rootPath: Path? = defaultRoot(),
    private val report: RenderReport = RenderReport(),
) {
    private val cache = HashMap<String, String?>()
    private val registeredUrls = HashSet<String>()
    private val root: Path? = rootPath?.let { path ->
        try {
            if (Files.isDirectory(path)) path.toRealPath() else null
        } catch (e: IOException) {
            null
        }
    }

    fun local(name: String): String? {
        return cache.getOrPut("local:$name") { loadLocal(name) }?.also { registeredUrls += it }
    }

    fun resolveRef(ref: String?): String? {
        val value = ref?.trim().orEmpty()
        if (value.isEmpty()) return null
        if (value in registeredUrls) return value
        return if (isHttpUrl(value)) remote(value) else local(value)
    }

    fun remote(url: String?): String? {
        val value = url?.trim().orEmpty()
        if (value.isEmpty()) return null
        if (!isHttpUrl(value)) {
            report.missing(value)
            return null
        }
        return cache.getOrPut("remote:$value") {
            val bytes = remote?.read(value)
            if (bytes == null || bytes.isEmpty()) {
                report.missing(value)
                null
            } else {
                register(bytes, value)
            }
        }
    }

    fun countryFlag(code: String?, x: Double, y: Double, height: Double): String? {
        var normalized = code?.trim()?.uppercase(Locale.ROOT).orEmpty()
        if (normalized.isEmpty() || !FLAG_CODE.matches(normalized)) return null

        val width = (height - 4) * 1.5
        if (normalized == "TW") {
            val image = local("flag-TW.png")
            if (image != null) {
                return Svg.image(x - 2, y + 4 + 2, width, height - 4, image, 1.0, "xMidYMid slice")
            }
            normalized = "CN"
        }

        if (normalized == "XX") {
            val image = local("Flags/XX") ?: local("Flags/XX.png")
            if (image != null) {
                return Svg.image(x - 2, y + 4 + 2, width, height - 4, image, 1.0, "xMidYMid slice")
            }
            report.missing("Flags/XX")
            return null
        }

        val body = flagBody(normalized)
        if (body == null) {
            report.missing("Flags/$normalized")
            return null
        }
        return "<g transform=\"translate(${Svg.number(x)} ${Svg.number(y)}) scale(${Svg.number(height / 30)})\">$body</g>"
    }

    fun localBanner(index: Int? = null): String? {
        val value = (index ?: ThreadLocalRandom.current().nextInt(1, 9)).coerceIn(1, 8)
        return local("Banner/c$value.webp") ?: local("card-default.webp")
    }

    fun flagBody(code: String?): String? {
        val normalized = code?.trim()?.uppercase(Locale.ROOT).orEmpty()
        if (normalized.isEmpty() || !FLAG_CODE.matches(normalized)) return null
        val candidates = listOf("Flags/$normalized", "Flags/$normalized.svg")
        for (candidate in candidates) {
            val path = resolveLocal(candidate) ?: continue
            val text = try {
                Files.readString(path)
            } catch (e: IOException) {
                continue
            }
            if (text.contains("<svg", ignoreCase = true)) {
                return Svg.bodyOf(text).takeIf { it.isNotBlank() }
            }
        }
        return null
    }

    fun readLocal(name: String): ByteArray? {
        val path = resolveLocal(name) ?: return null
        return try {
            Files.readAllBytes(path)
        } catch (e: IOException) {
            null
        }
    }

    private fun loadLocal(name: String): String? {
        val bytes = readLocal(name) ?: run {
            report.missing(name)
            return null
        }
        if (bytes.isEmpty()) {
            report.missing(name)
            return null
        }
        return register(bytes, name)
    }

    private fun register(bytes: ByteArray, source: String): String? {
        return try {
            registry.registerImage(bytes).also { registeredUrls += it }
        } catch (e: IllegalArgumentException) {
            report.missing(source)
            null
        }
    }

    private fun resolveLocal(name: String): Path? {
        val root = root ?: return null
        if (name.isBlank() || name.contains('\u0000') || name.contains('\\')) return null
        if (name.startsWith("/") || name.startsWith("\\")) return null
        val relative = try {
            Path.of(name)
        } catch (e: InvalidPathException) {
            return null
        }
        if (relative.isAbsolute) return null
        for (part in relative) {
            if (part.toString() == "..") return null
        }
        val resolved = root.resolve(relative).normalize()
        if (!resolved.startsWith(root)) return null
        val real = try {
            resolved.toRealPath()
        } catch (e: IOException) {
            return null
        }
        if (!real.startsWith(root)) return null
        if (!Files.isRegularFile(real)) return null
        return real
    }

    private fun isHttpUrl(value: String): Boolean {
        val uri = runCatching { java.net.URI(value) }.getOrNull() ?: return false
        if (uri.scheme?.lowercase(Locale.ROOT) !in setOf("http", "https")) return false
        if (uri.userInfo != null || uri.host.isNullOrBlank()) return false
        return true
    }

    companion object {
        private val FLAG_CODE = Regex("""[A-Z0-9]{2,3}""")

        fun defaultRoot(): Path? {
            val path = NowbotConfig.EXPORT_FILE_PATH ?: return null
            if (path.isBlank()) return null
            return runCatching { Path.of(path) }.getOrNull()
        }
    }
}

object Templates {
    fun read(name: String): String {
        val stream = Templates::class.java.getResourceAsStream("/templates/draw/$name")
            ?: throw IllegalStateException("template not found: $name")
        return stream.use { it.readBytes().toString(Charsets.UTF_8) }
    }
}
