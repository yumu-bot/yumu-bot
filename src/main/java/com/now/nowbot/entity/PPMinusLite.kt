package com.now.nowbot.entity

import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.util.DataUtil
import jakarta.persistence.*

@Entity
@Table(name = "pp_minus")
class PPMinusLite(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "user_id")
    var userID: Long? = null,
    @Column(name = "mode")
    var mode: Byte? = null,

    @Column(name = "record_time")
    var time: Long? = null,

    @Column(name = "user_pp")
    var userPP: Double? = null,
    @Column(name = "raw_pp")
    var rawPP: Double? = null,

    @Column(name = "accuracy")
    var accuracy: Double? = null,
    @Column(name = "total_hits")
    var totalHits: Long? = null,
    @Column(name = "play_count")
    var playCount: Long? = null,
    @Column(name = "play_time")
    var playTime: Long? = null,

    @Column(name = "top_pp")
    var topPP: Double? = null,
    @Column(name = "middle_pp")
    var middlePP: Double? = null,
    @Column(name = "bottom_pp")
    var bottomPP: Double? = null,

    @Column(name = "top_acc")
    var topAccuracy: Double? = null,
    @Column(name = "middle_acc")
    var middleAccuracy: Double? = null,
    @Column(name = "bottom_acc")
    var bottomAccuracy: Double? = null,

    @Column(name = "top_length")
    var topLength: Int? = null,
    @Column(name = "middle_length")
    var middleLength: Int? = null,
    @Column(name = "bottom_length")
    var bottomLength: Int? = null,

    @Column(name = "top_rate")
    var topPGRate: Double? = null,
    @Column(name = "middle_rate")
    var middlePGRate: Double? = null,
    @Column(name = "bottom_rate")
    var bottomPGRate: Double? = null,

    @Column(name = "count_d")
    var countD: Byte? = null,
    @Column(name = "count_c")
    var countC: Byte? = null,
    @Column(name = "count_b")
    var countB: Byte? = null,
    @Column(name = "count_a")
    var countA: Byte? = null,
    @Column(name = "count_s")
    var countS: Byte? = null,
    @Column(name = "count_ss")
    var countSS: Byte? = null,

    @Column(name = "count")
    var count: Byte? = null,
    @Column(name = "count_fc")
    var countFC: Byte? = null,

) {
    fun toLite(user: OsuUser, bests: List<LazerScore>): PPMinusLite {
        val lite = PPMinusLite()

        val mode = user.currentOsuMode

        lite.time = System.currentTimeMillis()
        lite.mode = mode.modeValue

        lite.userID = user.userID
        lite.userPP = user.pp
        lite.rawPP = user.pp - DataUtil.getBonusPP(user.pp, bests.map { it.pp })

        lite.accuracy = user.accuracy / 100.0
        lite.totalHits = user.totalHits
        lite.playCount = user.playCount
        lite.playTime = user.playTime

        val top: List<LazerScore> = bests.take(10)
        val middle: List<LazerScore> = if (bests.size >= 30) {
            bests.subList(bests.size / 2, bests.size / 2 + 5)
        } else if (bests.size >= 20) {
            bests.subList(9, 19)
        } else if (bests.size >= 10) {
            bests.takeLast(bests.size - 10)
        } else emptyList()
        val bottom: List<LazerScore> = if (bests.size >= 30) {
            bests.takeLast(10)
        } else emptyList()

        lite.topPP = top.map { it.pp }.average()
        lite.middlePP = middle.map { it.pp }.average()
        lite.bottomPP = bottom.map { it.pp }.average()

        lite.topAccuracy = top.map { it.accuracy }.average()
        lite.middleAccuracy = middle.map { it.accuracy }.average()
        lite.bottomAccuracy = bottom.map { it.accuracy }.average()

        lite.topLength = top.map { it.beatmap.hitLength ?: 0 }.average().toInt()
        lite.middleLength = middle.map { it.beatmap.hitLength ?: 0 }.average().toInt()
        lite.bottomLength = bottom.map { it.beatmap.hitLength ?: 0 }.average().toInt()

        lite.topPGRate = if (mode == OsuMode.MANIA) {
            top.map { it.statistics.perfect * 1.0 / it.statistics.great }.average()
        } else {
            0.0
        }
        lite.middlePGRate = if (mode == OsuMode.MANIA) {
            middle.map { it.statistics.perfect * 1.0 / it.statistics.great }.average()
        } else {
            0.0
        }
        lite.bottomPGRate = if (mode == OsuMode.MANIA) {
            bottom.map { it.statistics.perfect * 1.0 / it.statistics.great }.average()
        } else {
            0.0
        }

        lite.countD = bests.count { it.rank == "D" }.toByte()
        lite.countC = bests.count { it.rank == "C" }.toByte()
        lite.countB = bests.count { it.rank == "B" }.toByte()
        lite.countA = bests.count { it.rank == "A" }.toByte()
        lite.countS = bests.count { it.rank == "S" || it.rank == "SH" }.toByte()
        lite.countSS = bests.count { it.rank == "X" || it.rank == "XH" }.toByte()

        lite.count = bests.size.toByte()
        lite.countFC = bests.count { it.fullCombo }.toByte()

        return lite
    }
}