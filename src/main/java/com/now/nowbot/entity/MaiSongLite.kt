package com.now.nowbot.entity

import com.now.nowbot.model.json.MaiAlias
import com.now.nowbot.model.json.MaiFit
import com.now.nowbot.model.json.MaiRanking
import com.now.nowbot.model.json.MaiSong
import com.now.nowbot.util.DataUtil
import io.hypersistence.utils.hibernate.type.array.DoubleArrayType
import io.hypersistence.utils.hibernate.type.array.IntArrayType
import io.hypersistence.utils.hibernate.type.array.StringArrayType
import jakarta.persistence.*
import org.hibernate.annotations.Type

@Entity(name = "maimai_song")
@Table(indexes = [Index(name = "title_query", columnList = "query_text")])
class MaiSongLite(
    @Id
    var songID: Int? = null,

    @Column(columnDefinition = "text")
    var title: String,

    @Column(name = "query_text",columnDefinition = "text")
    var queryTitle: String = title,

    @Column(columnDefinition = "text")
    var type: String,

    @Type(DoubleArrayType::class)
    @Column(columnDefinition = "float[]")
    var star: DoubleArray,

    @Type(StringArrayType::class)
    @Column(columnDefinition = "text[]")
    var level: Array<String>,

    @Type(IntArrayType::class)
    @Column(columnDefinition = "integer[]")
    var chartIDs: IntArray,

    @Column(columnDefinition = "text")
    var songTitle: String,

    @Column(columnDefinition = "text")
    var songArtist: String,

    @Column(columnDefinition = "text")
    var songGenre: String,

    var songBPM: Int,

    @Column(columnDefinition = "text")
    var release: String,

    @Column(columnDefinition = "text")
    var version: String,

    var current: Boolean,
) {
    @Transient
    var charts: ArrayList<MaiChartLite>? = null

    fun toModel(): MaiSong = MaiSong().apply {
        val lite = this@MaiSongLite
        info = MaiSong.SongInfo().apply {
            title = lite.songTitle
            artist = lite.songArtist
            genre = lite.songGenre
            bpm = lite.songBPM
            release = lite.release
            version = lite.version
            current = lite.current
        }
        songID = lite.songID ?: 0
        title = lite.title
        type = lite.type
        star = lite.star.toList()
        chartIDs = lite.chartIDs.toList()
        level = lite.level.toList()

        val c = mutableListOf<MaiSong.MaiChart>()

        val chartLite = this@MaiSongLite.charts
        if (chartLite != null) {
            for (cl in chartLite) {
                val l = MaiSong.MaiChart()

                l.charter = cl.charter
                l.notes = if (lite.type == "DX") {
                    MaiSong.MaiChart.MaiNote(cl.notes.first(), cl.notes[1], cl.notes[2], cl.notes[3], cl.notes.last())
                } else {
                    MaiSong.MaiChart.MaiNote(cl.notes.first(), cl.notes[1], cl.notes[2], 0, cl.notes.last())
                }
                c.add(l)
            }
        }

        charts = c
    }

    companion object {
        @JvmStatic
        fun from(song: MaiSong): MaiSongLite {
            val queryTitle = DataUtil.getStandardisedString(song.title)

            val result = MaiSongLite(
                songID = song.songID,
                title = song.title,
                type = song.type,
                star = song.star.toDoubleArray(),
                level = song.level.toTypedArray(),
                chartIDs = song.chartIDs.toIntArray(),
                songTitle = song.info.title,
                songArtist = song.info.artist,
                songGenre = song.info.genre,
                songBPM = song.info.bpm,
                release = song.info.release,
                version = song.info.version,
                current = song.info.current
            )

            result.queryTitle = queryTitle

            result.charts = song.charts.mapIndexed { i, c ->
                MaiChartLite.from(result.chartIDs[i], c)
            }.toCollection(ArrayList())

            return result
        }
    }
}

@Entity(name = "maimai_chart")
class MaiChartLite(
    @Id
    var id: Int,

    @Type(IntArrayType::class)
    @Column(columnDefinition = "integer[]")
    var notes: IntArray,

    @Column(columnDefinition = "text")
    var charter: String
) {
    fun toModel(): MaiSong.MaiChart = with(MaiSong.MaiChart()) {
        val noteList = this@MaiChartLite.notes
        if (this@MaiChartLite.notes.size == 5) {
            this.notes = MaiSong.MaiChart.MaiNote(
                noteList[0],
                noteList[1],
                noteList[2],
                noteList[3],
                noteList[4],
            )
        } else {
            this.notes = MaiSong.MaiChart.MaiNote(
                noteList[0],
                noteList[1],
                noteList[2],
                0,
                noteList[3],
            )
        }
        charter = this@MaiChartLite.charter
        this
    }

    companion object {
        @JvmStatic
        fun from(id: Int, chart: MaiSong.MaiChart): MaiChartLite {
            val notes = if (chart.notes.touch == 0) with(IntArray(4)) {
                this[0] = chart.notes.tap
                this[1] = chart.notes.hold
                this[2] = chart.notes.slide
                this[3] = chart.notes.break_
                this
            } else with(IntArray(5)) {
                this[0] = chart.notes.tap
                this[1] = chart.notes.hold
                this[2] = chart.notes.slide
                this[3] = chart.notes.touch
                this[4] = chart.notes.break_
                this
            }

            return MaiChartLite(
                id = id,
                notes = notes,
                charter = chart.charter
            )
        }
    }
}

@Entity(name = "maimai_fit_chart")
class MaiFitChartLite(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Int? = null,

    var sort: Int,

    var songID: Int,

    var count: Int,

    @Column(columnDefinition = "text")
    var level: String,

    var fit: Double,

    var achievements: Double,

    var score: Double,

    @Column(name = "standard_deviation")
    var standardDeviation: Double,

    @Type(DoubleArrayType::class)
    @Column(name = "distribution", columnDefinition = "float[]")
    var distribution: DoubleArray,

    @Type(DoubleArrayType::class)
    @Column(name = "fc_distribution", columnDefinition = "float[]")
    var fullComboDistribution: DoubleArray,
) {
    fun toModel(): MaiFit.ChartData {
        val data = MaiFit.ChartData()
        data.count = count
        data.level = level
        data.fit = fit
        data.achievements = achievements
        data.score = score
        data.standardDeviation = standardDeviation
        data.distribution = distribution.toList()
        data.fullComboDistribution = fullComboDistribution.toList()
        return data
    }

    companion object {
        @JvmStatic
        fun from(sid: String, sort: Int, chart: MaiFit.ChartData): MaiFitChartLite {
            return MaiFitChartLite(
                sort = sort,
                songID = sid.toInt(),
                count = chart.count,
                level = chart.level,
                fit = chart.fit,
                achievements = chart.achievements,
                score = chart.score,
                standardDeviation = chart.standardDeviation,
                distribution = chart.distribution.toDoubleArray(),
                fullComboDistribution = chart.fullComboDistribution.toDoubleArray()
            )
        }
    }
}

@Entity(name = "maimai_fit_diff")
class MaiFitDiffLite(
    @Id
    @Column(columnDefinition = "text")
    var id: String,

    var achievements: Double,

    @Type(DoubleArrayType::class)
    @Column(name = "distribution", columnDefinition = "float[]")
    var distribution: DoubleArray,

    @Type(DoubleArrayType::class)
    @Column(name = "fc_distribution", columnDefinition = "float[]")
    var fullComboDistribution: DoubleArray,
) {
    fun toModel(): MaiFit.DiffData {
        val data = MaiFit.DiffData()
        data.achievements = achievements
        data.distribution = distribution.toList()
        data.fullComboDistribution = fullComboDistribution.toList()
        return data
    }

    companion object {
        @JvmStatic
        fun from(id: String, diff: MaiFit.DiffData): MaiFitDiffLite {
            return MaiFitDiffLite(
                id = id,
                achievements = diff.achievements,
                distribution = diff.distribution.toDoubleArray(),
                fullComboDistribution = diff.fullComboDistribution.toDoubleArray()
            )
        }
    }
}

@Entity(name = "maimai_user_rank")
class MaiRankingLite(
    @Id
    @Column(columnDefinition = "text")
    var name: String,

    var rating: Int,
) {
    fun toModel(): MaiRanking {
        return MaiRanking(
            name, rating
        )
    }

    companion object {
        fun from(model: MaiRanking): MaiRankingLite {
            if (model.name.isBlank()) throw IllegalArgumentException("name not be empty")
            return MaiRankingLite(model.name, model.rating)
        }
    }
}

@Entity(name = "maimai_alias")
class MaiAliasLite(
    @Id
    @Column(columnDefinition = "integer")
    var id: Int,

    @Column(columnDefinition = "text[]")
    var alias: List<String>,
) {
    fun toModel(): MaiAlias {
        return MaiAlias(
            id, alias
        )
    }

    companion object {
        fun from(model: MaiAlias): MaiAliasLite {
            if (model.alias.isEmpty()) throw IllegalArgumentException("alias not be empty")
            return MaiAliasLite(model.songID, model.alias)
        }
    }
}