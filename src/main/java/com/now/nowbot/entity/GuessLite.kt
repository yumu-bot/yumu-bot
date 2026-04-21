package com.now.nowbot.entity

import com.now.nowbot.service.messageServiceImpl.GuessService
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

@Entity
@Table(name = "guess")
class GuessLite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "guess_id")
    var guessID: Long? = null

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "beatmaps", columnDefinition = "int8[]")
    var beatmaps: LongArray = longArrayOf()

    @Column(name = "artist")
    var artist: Boolean = false

    @Column(name = "unicode")
    var unicode: Boolean = false

    @Column(name = "start_time")
    var startTime: LocalDateTime = LocalDateTime.now()

    @Column(name = "played_at")
    var lastPlayedTime: LocalDateTime = startTime

    @Column(name = "decrypted")
    var decrypted: Int = 0

    // 一对多关系
    @OneToMany(
        mappedBy = "guess",
        cascade = [CascadeType.ALL],
        fetch = FetchType.LAZY,
        orphanRemoval = true,
        targetEntity = GuesserLite::class
    )
    var guessers: MutableList<GuesserLite> = mutableListOf()

    companion object {
        fun from(game: GuessService.GuessGame): GuessLite? {
            val bs = game.decrypted.mapIndexed { index, decrypt ->
                if (decrypt < 0) {
                    game.scores[index].beatmapID
                } else {
                    null
                }
            }.filterNotNull()

            val gs = game.results.mapNotNull { gr ->
                gr?.let { r -> GuesserLite.from(r) }
            }.toMutableList()

            if (bs.isEmpty() || gs.isEmpty()) return null

            return GuessLite().apply {
                beatmaps = bs.toLongArray()
                artist = game.artist
                unicode = game.unicode
                startTime = game.startTime
                lastPlayedTime = game.lastPlayedTime
                decrypted = game.decrypted.foldIndexed(0) { index, acc, value ->
                    if (value < 0) {
                        // 如果小于0，将 1 左移 index 位，然后与当前结果进行或运算
                        acc or (1 shl index)
                    } else {
                        acc
                    }
                }

                guessers = gs.onEach { it.guess = this }
            }
        }
    }
}

@Entity
@Table(name = "guesser", indexes = [
    Index(name = "idx_guesser_id", columnList = "guesser_id, beatmap_id, group_id"),
    Index(name = "idx_guesser_guess_id", columnList = "guess_id")
])
class GuesserLite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "result_id")
    var resultID: Long? = null

    @Column(name = "guesser_id")
    var guesserID: Long = 0

    @Column(name = "group_id")
    var groupID: Long = 0

    @Column(name = "beatmap_id")
    var beatmapID: Long = 0

    @Column(name = "reward")
    var reward: Int = 0

    // 关联字段
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guess_id")
    var guess: GuessLite? = null

    companion object {
        fun from(guesser: GuessService.Guesser): GuesserLite {
            return GuesserLite().apply {
                guesserID = guesser.guesserID
                groupID = guesser.groupID
                beatmapID = guesser.beatmapID
                reward = guesser.reward
            }
        }
    }
}