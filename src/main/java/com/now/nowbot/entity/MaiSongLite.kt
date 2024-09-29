package com.now.nowbot.entity

import com.now.nowbot.model.json.MaiFit
import com.now.nowbot.model.json.MaiRanking
import com.now.nowbot.model.json.MaiSong
import io.hypersistence.utils.hibernate.type.array.DoubleArrayType
import io.hypersistence.utils.hibernate.type.array.IntArrayType
import io.hypersistence.utils.hibernate.type.array.StringArrayType
import jakarta.persistence.*
import org.hibernate.annotations.Type

@Entity(name = "maimai_song")
class MaiSongLite(
    @Id
    var songID: Int? = null,

    @Column(columnDefinition = "text")
    var title: String,

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

    @Column(columnDefinition = "text[]")
    var songTitle: String,
    @Column(columnDefinition = "text[]")
    var songArtist: String,
    @Column(columnDefinition = "text[]")
    var songGenre: String,
    var songBPM: Int,
    @Column(columnDefinition = "text[]")
    var release: String,
    @Column(columnDefinition = "text[]")
    var version: String,
    var current: Boolean,
) {
    @Transient
    var charts: ArrayList<MaiChartLite>? = null

    fun toModel(): MaiSong = with(MaiSong()) {
        this.info = with(MaiSong.SongInfo()) {
            title = songTitle
            artist = songArtist
            genre = songGenre
            bpm = songBPM
            release = this@MaiSongLite.release
            version = this@MaiSongLite.version
            current = this@MaiSongLite.current
            this
        }
        val dbCharts = this@MaiSongLite.charts
        if (dbCharts != null) {
            this.charts = dbCharts.map {
                it.toModel()
            }
        }
        this
    }

    companion object {
        @JvmStatic
        fun from(song: MaiSong): MaiSongLite {
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
                this
            } else with(IntArray(5)) {
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