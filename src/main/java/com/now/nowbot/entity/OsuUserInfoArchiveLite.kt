package com.now.nowbot.entity

import com.now.nowbot.model.enums.OsuMode
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity @Table(
    name = "osu_user_info_archive",
    indexes = [
        Index(name = "index_osu_id", columnList = "osu_id"),
        Index(name = "index_usermod_find", columnList = "osu_id,mode"),
        Index(name = "index_user_time", columnList = "time")
    ]
)
class OsuUserInfoArchiveLite {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null

    @Column(name = "osu_id") var userID: Long = 0L

    @Column(name = "mode") var mode: OsuMode = OsuMode.DEFAULT

    @Column(name = "time") var time: LocalDateTime = LocalDateTime.now()

    // ',' 分割
    //@Lob
    @Column(name = "rank_history", columnDefinition = "TEXT") var rankHistory: String? = null

    //等级
    @Column(name = "level_current") var levelCurrent: Int = 0

    @Column(name = "level_progress") var levelProgress: Int = 0

    //rank
    @Column(name = "global_rank") var globalRank: Long? = null

    @Column(name = "country_rank") var countryRank: Long? = null

    @Column(name = "pp") var pp: Double = 0.0

    @Column(name = "hit_accuracy") var accuracy: Double = 0.0

    @Column(name = "play_count") var playCount: Long = 0

    @Column(name = "play_time") var playTime: Long = 0

    @Column(name = "ranked_score") var rankedScore: Long = 0

    @Column(name = "total_score") var totalScore: Long = 0

    @Column(name = "total_hits") var totalHits: Long = 0

    @Column(name = "is_ranked") var isRanked: Boolean = false

    @Column(name = "grade_counts_ss") var countSS: Int = 0

    @Column(name = "grade_counts_ssh") var countSSH: Int = 0

    @Column(name = "grade_counts_s") var countS: Int = 0

    @Column(name = "grade_counts_sh") var countSH: Int = 0

    @Column(name = "grade_counts_a") var countA: Int = 0

    @Column(name = "beatmap_playcount") var beatmapPlaycount: Int = 0

    @Column(name = "replays_watched")
    var replaysWatched: Int = 0

    @Column(name = "maximum_combo")
    var maximumCombo: Int = 0

    interface InfoArchive {
        @get:Column(name = "osu_id")
        val userID: Long
        val modeShort: Short
        val playCount: Long
        val mode: OsuMode
            get () {
                return when (this.modeShort.toInt()) {
                    0 -> OsuMode.OSU
                    1 -> OsuMode.TAIKO
                    2 -> OsuMode.CATCH
                    3 -> OsuMode.MANIA
                    else -> OsuMode.DEFAULT
                }
        }
    }
}
