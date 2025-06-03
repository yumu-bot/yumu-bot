package com.now.nowbot.model.osu

import com.now.nowbot.util.lzma.LZMAInputStream
import java.io.ByteArrayInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Replay constructor(bf: ByteBuffer) {
    // 0 = osu!, 1 = osu!taiko, 2 = osu!catch, 3 = osu!mania
    var mode: Byte

    //创建该回放文件的游戏版本 例如：20131216
    var version: Int
    var beatmapHash: String
    var username: String

    // 回放文件的 MD5 hash
    var hash: String
    var n_300: Short
    var n_100: Short
    var n_50: Short
    var n_geki: Short
    var n_katu: Short
    var n_miss: Short
    var score: Int
    var combo: Short

    // full combo（1 = 没有Miss和断滑条，并且没有提前完成的滑条）
    var isPerfect: Boolean
    var mods: Int
    var healthPoint: Map<Int, Float>? = null

    // 时间戳 注意这个是以公元0年开始, 不是1970年的时间戳
    var date: Long
    var dataLength: Int
    var hits: List<Hit>

    // 与 url 中的末尾数字不是同一个
    var scoreID: Long
    var targetPractice: Double = 0.0

    init {
        if (bf.order() == ByteOrder.BIG_ENDIAN) {
            bf.order(ByteOrder.LITTLE_ENDIAN)
        }
        mode = bf.get()
        version = bf.getInt()
        beatmapHash = readString(bf)
        username = readString(bf)
        hash = readString(bf)
        n_300 = bf.getShort()
        n_100 = bf.getShort()
        n_50 = bf.getShort()
        n_geki = bf.getShort()
        n_katu = bf.getShort()
        n_miss = bf.getShort()
        score = bf.getInt()
        combo = bf.getShort()
        isPerfect = bf.get().toInt() == 1
        mods = bf.getInt()
        val Hp = readString(bf)
        date = bf.getLong()
        dataLength = bf.getInt()
        val data = ByteArray(dataLength)
        bf[data, 0, dataLength]
        hits = parseHits(data)
        scoreID = bf.getLong()
        if (bf.limit() >= 8 + bf.position()) {
            targetPractice = bf.getDouble()
        }
        if (Hp.isNotBlank()) {
            healthPoint = readHp(Hp)
        }
    }

    class Hit(
        var offset: Long,
        //        鼠标的X坐标（从0到512）
        var x: Float,
        //        鼠标的Y坐标（从0到384）
        var y: Float,
        //鼠标、键盘按键的组合（M1 = 1, M2 = 2, K1 = 4, K2 = 8, 烟雾 = 16）（K1 总是与 M1 一起使用，K2 总是与 M2 一起使用。所以 1+4=5 2+8=10。）
        var hit: Int
    )

    companion object {

        private fun readString(bf: ByteBuffer): String {
            //        int p = (0xFF & e[offset]) |
            //                (0xFF & e[offset+1])<<8 |
            //                (0xFF & e[offset+2])<<16 |
            //                (0xFF & e[offset+3])<<24 ;
            if (bf.get().toInt() == 11) {
                // 读取第二位 可变长int 值string byte长度
                val strLength = readLength(bf)
                //得到长度 读取string byte
                val strData = ByteArray(strLength)
                bf[strData, 0, strLength]
                //转换string
                return String(strData)
            } else {
                return ""
            }
        }

        private fun parseHits(byteArray: ByteArray): List<Hit> {
            return try {
                val s = String(LZMAInputStream(ByteArrayInputStream(byteArray)).readAllBytes())

                if (s.isNullOrBlank()) return emptyList()

                val lines = s.split(",".toRegex()).dropLastWhile { it.isEmpty() }

                val hits = mutableListOf<Hit>()

                for (line in lines) {
                    val split = line.split("\\|".toRegex())
                    hits.add(Hit(split[0].toLong(), split[1].toFloat(), split[2].toFloat(), split[3].toInt()))
                }

                hits.toList()
            } catch (e: IOException) {
                e.printStackTrace()

                emptyList()
            }
        }

        private fun readHp(data: String): Map<Int, Float> {
            val lines = data.split(",".toRegex()).map { it.trim() }

            return  lines.map {
                val k = it.split("\\|".toRegex()).map { it.trim() }

                k[0].toInt() to k[1].toFloat()
            }.toMap()
        }

        private fun readLength(bf: ByteBuffer): Int {
            var result = 0
            var shift = 0
            var b: Byte
            do {
                b = bf.get()
                result = result or ((b.toInt() and 0x7F) shl shift)
                shift += 7
            } while ((0x80 and b.toInt()) != 0)
            return result
        }

        fun readByteToRep(buffer: ByteBuffer): Replay {
            return Replay(buffer)
        }

        fun readByteToRep(buffer: ByteArray): Replay {
            return Replay(ByteBuffer.wrap(buffer))
        }
    }
}
