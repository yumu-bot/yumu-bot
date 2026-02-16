package com.now.nowbot.model.beatmapParse

import com.now.nowbot.entity.BeatmapFileLite
import com.now.nowbot.model.beatmapParse.parse.*
import com.now.nowbot.model.enums.OsuMode
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.IOException

class OsuFile @Throws(IOException::class) constructor(private val reader: BufferedReader) {

    private val general: BeatmapGeneral

    val mode: OsuMode
        get() = general.mode ?: OsuMode.OSU

    init {
        val versionStr = reader.readLine()
        // 修正：确保 versionStr 不为 null 且正确 trim
        val versionInt = if (versionStr != null && versionStr.trim().startsWith("osu file format v")) {
            versionStr.substring(17).trim().toInt()
        } else {
            throw RuntimeException("解析错误,文件无效")
        }

        this.general = BeatmapGeneral(versionInt)

        var line: String?
        // 修正：安全跳过空白行直到找到 [General]
        while (true) {
            line = reader.readLine()
            if (line == null) throw RuntimeException("解析错误,缺失 [General] 块")
            if (line.isNotBlank()) break
        }

        if (line.startsWith("[General]")) {
            parseGeneral(reader)
        } else {
            throw RuntimeException("解析错误,缺失 [General] 块")
        }
    }

    @Throws(IOException::class)
    private fun parseGeneral(reader: BufferedReader) {
        while (true) {
            val line = reader.readLine()?.takeIf { it.isNotBlank() } ?: break
            val entity = line.split(":", limit = 2)
            if (entity.size != 2) continue

            val key = entity[0].trim()
            val value = entity[1].trim()

            when (key) {
                "Mode" -> general.mode = OsuMode.getMode(value)
                "StackLeniency" -> general.stackLeniency = value.toDoubleOrNull() ?: 0.0
                "SampleSet" -> general.sampleSet = value
            }
        }
    }

    @Throws(IOException::class)
    fun getOsu(): OsuBeatmapAttributes {
        return when (mode) {
            OsuMode.OSU -> OsuBeatmapAttributes(reader, general)
            OsuMode.TAIKO -> TaikoBeatmapAttributes(reader, general)
            OsuMode.CATCH -> CatchBeatmapAttributes(reader, general)
            OsuMode.MANIA -> ManiaBeatmapAttributes(reader, general)
            else -> throw RuntimeException("mode type error")
        }
    }

    @Throws(IOException::class)
    fun getCatch(): ManiaBeatmapAttributes {
        if (mode != OsuMode.CATCH && mode != OsuMode.OSU) throw RuntimeException("mode error")
        return ManiaBeatmapAttributes(reader, general)
    }

    @Throws(IOException::class)
    fun getTaiko(): ManiaBeatmapAttributes {
        if (mode != OsuMode.TAIKO && mode != OsuMode.OSU) throw RuntimeException("mode error")
        return ManiaBeatmapAttributes(reader, general)
    }

    @Throws(IOException::class)
    fun getMania(): ManiaBeatmapAttributes {
        if (mode != OsuMode.MANIA && mode != OsuMode.OSU) throw RuntimeException("mode error")
        return ManiaBeatmapAttributes(reader, general)
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(OsuFile::class.java.name)

        /**
         * 对应 Java 的 OsuFile.getInstance(String osuFileStr)
         */
        @JvmStatic
        @Throws(IOException::class)
        fun getInstance(osuFileStr: String): OsuFile {
            val reader = BufferedReader(
                java.io.InputStreamReader(
                    java.io.ByteArrayInputStream(osuFileStr.toByteArray(java.nio.charset.StandardCharsets.UTF_8))
                )
            )
            return OsuFile(reader)
        }

        /**
         * 对应 Java 的 OsuFile.getInstance(BufferedReader read)
         */
        @JvmStatic
        @Throws(IOException::class)
        fun getInstance(read: BufferedReader): OsuFile {
            return OsuFile(read)
        }


        @Throws(IOException::class)
        fun parseInfo(reader: BufferedReader): BeatmapFileLite {

            return reader.use { r ->
                val versionStr = r.readLine()
                if (versionStr == null || !versionStr.trim().startsWith("osu file format v")) {
                    log.error("解析错误,文件无效 第一行为:{}", versionStr)
                    throw RuntimeException("解析错误,文件无效")
                }
                if (versionStr.endsWith("v3")) {
                    throw RuntimeException("不支持v3版本的解析")
                }

                val bf = BeatmapFileLite()
                val info = mutableMapOf<String, String?>(
                    "AudioFilename" to null,
                    "Mode" to null,
                    "BeatmapID" to null,
                    "BeatmapSetID" to null
                )

                var line: String?
                // 修正：模仿 Java 的 while ((line = read.readLine()) != null)
                while (reader.readLine().also { line = it } != null) {
                    val currentLine = line!!
                    if (currentLine.startsWith("[General]") || currentLine.startsWith("[Metadata]")) {
                        parseAny(reader, info)
                    }
                    if (currentLine.startsWith("[Events]")) {
                        break
                    }
                }

                // 解析 Background 逻辑
                while (reader.readLine().also { line = it } != null) {
                    val currentLine = line!!
                    if (currentLine.isBlank() || currentLine.startsWith("//")) continue
                    if (currentLine.startsWith("[")) break

                    val lineSplit = currentLine.split(",")
                    if (lineSplit.size >= 3 && lineSplit[0] == "0" && lineSplit[1] == "0" &&
                        lineSplit[2].startsWith("\"") && lineSplit[2].endsWith("\"")
                    ) {

                        val bgStr = lineSplit[2]
                        bf.background = bgStr.substring(1, bgStr.length - 1)
                        break
                    }
                }

                info["AudioFilename"]?.let { bf.audio = it }
                info["Mode"]?.let { bf.modeInt = it.toInt() }
                info["BeatmapID"]?.let { bf.bid = it.toLong() }
                info["BeatmapSetID"]?.let { bf.sid = it.toLong() }

                return@use bf
            }
        }

        @Throws(IOException::class)
        private fun parseAny(reader: BufferedReader, parseMap: MutableMap<String, String?>) {
            while (true) {
                val line = reader.readLine()?.takeIf { it.isNotBlank() } ?: break
                val entity = line.split(":".toRegex(), limit = 2)
                if (entity.size != 2) continue

                val key = entity[0].trim()
                if (parseMap.containsKey(key)) {
                    parseMap[key] = entity[1].trim()
                }
            }
        }
    }
}