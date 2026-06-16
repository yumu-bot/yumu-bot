package com.now.nowbot.qq.local.contact

import com.now.nowbot.config.NowbotConfig
import com.now.nowbot.qq.contact.Contact
import com.now.nowbot.qq.message.*
import com.now.nowbot.util.DataUtil.getImageExtension
import java.io.IOException
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path

open class LocalContact(override val contactID: Long = 1340691940, override val name: String? = null) : Contact {

    override fun sendMessage(msg: MessageChain): MessageReceipt {
        val contact = this
        val message = msg.messageList.joinToString("") {
            when (it) {
                is AtMessage -> "[@${it.target}]"
                is ReplyMessage -> "[回复:${it.id}]"
                is TextMessage -> it.toString()
                is VoiceMessage -> {
                    val path = saveFile("${System.currentTimeMillis()}.mp3", it.data)
                    "[voice: ${path}]"
                }

                is ImageMessage -> {
                    val localPath = if (it.isByteArray) {
                        val ext = it.data!!.getImageExtension()
                        saveFile("${System.currentTimeMillis()}$ext", it.data)
                    } else if (it.isUrl) {
                        downloadFile(it.path!!)
                    } else {
                        copyFile(Path.of(URI.create(it.path!!.replace("\\\\".toRegex(), "/"))))
                    }

                    "[图片: ${localPath}]"
                }

                else -> "[未知类型]"
            }
        }

        Contact.log.info("bot: \"{}\"", message)

        return object : MessageReceipt() {
            override fun recall() {
                Contact.log.info("bot 撤回消息: {}", message)
            }

            override fun recallIn(time: Long) {
                Thread.startVirtualThread {
                    try {
                        Thread.sleep(time)
                        recall()
                    } catch (_: InterruptedException) { }
                }
            }

            override fun reply(): ReplyMessage {
                return ReplyMessage(0, "本地消息")
            }

            override fun getTarget(): Contact {
                return contact
            }
        }
    }

    fun saveFile(name: String, data: ByteArray): String {
        val path = Path.of(NowbotConfig.RUN_PATH, "debug")
        try {
            if (!Files.isDirectory(path)) Files.createDirectories(path)
            val nPath = path.resolve(name)
            Files.write(nPath, data)
            return nPath.toAbsolutePath().toString()
        } catch (_: IOException) {
            Contact.log.info("保存文件：创建文件夹 {} 失败", path.toAbsolutePath())
            return "err"
        }
    }

    private fun downloadFile(url: String): String {
        val path = Path.of(NowbotConfig.RUN_PATH, "debug")
        try {
            if (!Files.isDirectory(path)) Files.createDirectories(path)
            val urlObj = URI.create(url).toURL()
            val connection = urlObj.openConnection()
            val data = connection.getInputStream().readAllBytes()
            val nPath: Path = path.resolve("${System.currentTimeMillis()}${data.getImageExtension()}"
            )
            Files.write(nPath, data)
            return nPath.toAbsolutePath().toString()
        } catch (_: IOException) {
            Contact.log.info("下载文件：创建文件夹 {} 失败", path.toAbsolutePath())
            return "err"
        }
    }

    private fun Path.getExtension(): String {
        // 1. 先尝试获取真实的 MIME 类型（例如 "image/png", "image/jpeg"）
        val mimeType = try {
            Files.probeContentType(this)
        } catch (_: Exception) {
            null
        }

        // 2. 根据 MIME 类型返回后缀
        return when (mimeType) {
            "image/jpeg", "image/jpg" -> ".jpg"
            "image/png" -> ".png"
            "image/gif" -> ".gif"
            "image/webp" -> ".webp"
            "image/bmp" -> ".bmp"
            else -> {
                // 3. 如果探测失败，尝试切分原文件名获取后缀（兜底）
                val fileName = this.fileName.toString()
                val dotIndex = fileName.lastIndexOf('.')
                if (dotIndex in 1 until fileName.length - 1) {
                    fileName.substring(dotIndex)
                } else {
                    ".jpg" // 彻底无法识别时，默认 .jpg
                }
            }
        }
    }

    private fun copyFile(source: Path): String {
        val path = Path.of(NowbotConfig.RUN_PATH, "debug")
        try {
            if (!Files.isDirectory(path)) Files.createDirectories(path)

            val ext = source.getExtension()

            val nPath: Path = path.resolve("${System.currentTimeMillis()}$ext")
            Files.copy(source, nPath)
            return nPath.toAbsolutePath().toString()
        } catch (_: IOException) {
            Contact.log.info("复制文件：创建文件夹 {} 失败", path.toAbsolutePath())
            return "err"
        }
    }
}
