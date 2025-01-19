package com.now.nowbot.entity

import com.now.nowbot.newbie.mapper.NewbieDao
import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(
    name = "newbie_play_count",
    indexes = [Index(name = "index_date", columnList = "record_date"), Index(name = "index_uid", columnList = "uid")]
)
class NewbiePlayCount(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "record_date", columnDefinition = "date")
    var date: LocalDate? = LocalDate.now(),

    @Column(name = "uid")
    var uid: Int? = null,

    var pp: Float? = null,

    var playTime: Int? = null,

    var playCount: Int? = null,

    var playHits: Int? = null,
) {
    constructor(record: NewbieDao.UserCount) : this(
        uid = record.id,
        pp = record.pp,
        playTime = record.playTime,
        playCount = record.playCount,
        playHits = record.tth,
    )

    interface UserHistoryResult {
        val data: Int
        val pc: Int
        val tth: Int
        val ppMin: Float
        val ppMax: Float
    }

    interface UserListResult {
        val uid: Int
        var pp: Float?
        var playCount: Int?
        var playHits: Int?
    }
}