package com.now.nowbot.entity

import com.now.nowbot.model.maimai.*
import com.now.nowbot.util.DataUtil
import io.hypersistence.utils.hibernate.type.array.DoubleArrayType
import io.hypersistence.utils.hibernate.type.array.IntArrayType
import io.hypersistence.utils.hibernate.type.array.StringArrayType
import jakarta.persistence.*
import org.hibernate.annotations.Type
import kotlin.math.roundToInt

@Entity(name = "maimai_song")
@Table(indexes = [Index(name = "mai_title_query", columnList = "query_text")])
class MaiSongLite(
    @Id
    var songID: Int? = null,

    @Column(columnDefinition = "text")
    var title: String,

    @Column(name = "query_text", columnDefinition = "text")
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

@Entity(name = "maimai_rank")
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

@Entity(name = "chunithm_alias")
class ChuAliasLite(
    @Id
    @Column(columnDefinition = "integer")
    var id: Int,

    @Column(columnDefinition = "text[]")
    var alias: List<String>,
) {
    fun toModel(): ChuAlias {
        return ChuAlias(
            id, alias
        )
    }

    companion object {
        fun from(model: ChuAlias): ChuAliasLite {
            if (model.alias.isEmpty()) throw IllegalArgumentException("alias not be empty")
            return ChuAliasLite(model.songID, model.alias)
        }
    }
}

@Entity(name = "chunithm_song")
@Table(indexes = [Index(name = "chu_title_query", columnList = "query_text")])
class ChuSongLite(
    @Id
    @Column(columnDefinition = "integer")
    var songID: Int,

    @Column(columnDefinition = "text")
    var title: String,

    @Column(name = "query_text", columnDefinition = "text")
    var queryTitle: String = title,

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

    @Column(columnDefinition = "integer")
    var songBPM: Int,

    @Column(columnDefinition = "text")
    var version: String,

    ) {
    @Transient
    var charts: ArrayList<ChuChartLite>? = null

    fun toModel(): ChuSong = ChuSong().apply {
        val lite = this@ChuSongLite

        info = ChuSong.SongInfo().apply {
            title = lite.songTitle
            artist = lite.songArtist
            genre = lite.songGenre
            bpm = lite.songBPM
            version = lite.version
        }

        songID = lite.songID
        title = lite.title
        star = lite.star.toList()
        chartIDs = lite.chartIDs.toList()
        level = lite.level.toList()

        val c = mutableListOf<ChuSong.ChuChart>()

        val chartLite = this@ChuSongLite.charts
        if (chartLite != null) {
            for (cl in chartLite) {
                val l = ChuSong.ChuChart()

                l.charter = cl.charter
                l.combo = cl.combo
                c.add(l)
            }
        }

        charts = c
    }

    companion object {
        @JvmStatic
        fun from(song: ChuSong): ChuSongLite {
            val queryTitle = DataUtil.getStandardisedString(song.title)

            val result = ChuSongLite(
                songID = song.songID,
                title = song.title,
                star = song.star.toDoubleArray(),
                level = song.level.toTypedArray(),
                chartIDs = song.chartIDs.toIntArray(),
                songTitle = song.info.title,
                songArtist = song.info.artist,
                songGenre = song.info.genre,
                songBPM = song.info.bpm,
                version = song.info.version,
            )

            result.queryTitle = queryTitle

            result.charts = song.charts.mapIndexed { i, c ->
                ChuChartLite.from(result.chartIDs[i], c)
            }.toCollection(ArrayList())

            return result
        }
    }
}

@Entity(name = "chunithm_chart")
class ChuChartLite(
    @Id
    var id: Int,

    @Column(columnDefinition = "integer")
    var combo: Int,

    @Column(columnDefinition = "text")
    var charter: String
) {

    companion object {
        @JvmStatic
        fun from(id: Int, chart: ChuSong.ChuChart): ChuChartLite {
            return ChuChartLite(
                id = id,
                combo = chart.combo,
                charter = chart.charter
            )
        }
    }
}

@Entity
@Table(
    name = "lx_maimai_song",
    indexes = [Index(name = "lx_mai_title_query", columnList = "query_text")]
)
class LxMaiSongLite {
    @Id
    var songID: Int? = null

    @Column(columnDefinition = "text")
    var title: String = ""

    @Column(name = "query_text", columnDefinition = "text")
    var queryTitle: String = title

    @Column(columnDefinition = "text")
    var artist: String = ""

    @Column(columnDefinition = "text")
    var genre: String = ""

    var bpm: Int = 0

    var version: Int = 0

    // 一对多关系
    @OneToMany(
        mappedBy = "song",
        cascade = [CascadeType.ALL],
        fetch = FetchType.LAZY,
        orphanRemoval = true,
        targetEntity = LxMaiDifficultyLite::class
    )
    var difficulties: MutableList<LxMaiDifficultyLite> = mutableListOf()

    // 计算属性，按谱面类型过滤
    val standard: List<LxMaiDifficultyLite>
        get() = difficulties.filter { it.chartType == "standard" }

    val deluxe: List<LxMaiDifficultyLite>
        get() = difficulties.filter { it.chartType == "deluxe" }

    val utage: List<LxMaiDifficultyLite>
        get() = difficulties.filter { it.chartType == "utage" }

    // 便利方法
    fun addDifficulty(difficulty: LxMaiDifficultyLite) {
        difficulties.add(difficulty)
        difficulty.song = this
    }

    fun addDifficulties(newDifficulties: Collection<LxMaiDifficultyLite>) {
        newDifficulties.forEach { addDifficulty(it) }
    }

    fun removeDifficulty(difficulty: LxMaiDifficultyLite) {
        difficulties.remove(difficulty)
        difficulty.song = null
    }

    fun toModel(): LxMaiSong = LxMaiSong().apply {
        val lite = this@LxMaiSongLite

        songID = lite.songID!!
        title = lite.title
        artist = lite.artist
        genre = lite.genre
        bpm = lite.bpm
        version = lite.version
        difficulties = LxMaiDiff(
            standard.map { it.toModel() },
            deluxe.map { it.toModel() },
            utage.map { it.toModel() },
        )
    }

    companion object {
        fun from(song: LxMaiSong): LxMaiSongLite {
            val query = DataUtil.getStandardisedString(song.title)

            return LxMaiSongLite().apply {
                songID = song.songID
                title = song.title
                queryTitle = query
                artist = song.artist
                genre = song.genre
                bpm = song.bpm
                version = song.version

                // 合并所有难度类型// 修复：使用便利方法添加，确保双向关系正确
                addDifficulties(
                    listOf(
                        song.difficulties.standard.map {
                            LxMaiDifficultyLite.from(it, "standard")
                        },
                        song.difficulties.deluxe.map {
                            LxMaiDifficultyLite.from(it, "deluxe")
                        },
                        song.difficulties.utage.map {
                            LxMaiDifficultyLite.from(it, "utage")
                        }
                    ).flatten()
                )
            }
        }
    }
}

@Entity
@Table(name = "lx_maimai_difficulty")
class LxMaiDifficultyLite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    // 谱面类型：standard, deluxe, utage
    @Column(name = "chart_type", columnDefinition = "VARCHAR(10)")
    var chartType: String = ""

    @Column(columnDefinition = "TEXT")
    var type: String = ""

    var difficulty: Byte = 0

    @Column(columnDefinition = "VARCHAR(5)")
    var level: String = ""

    // x10
    @Column(name = "level_value", columnDefinition = "SMALLINT")
    var levelValue: Short = 0

    @Column(name = "note_designer")
    var noteDesigner: String = ""

    var version: Int = 0

    @Type(IntArrayType::class)
    @Column(columnDefinition = "INTEGER[]")
    var notes: IntArray = intArrayOf()

    @Column(columnDefinition = "VARCHAR(4)", nullable = true)
    var kanji: String? = null

    @Column(columnDefinition = "TEXT", nullable = true)
    var description: String? = null

    @Column(name = "is_buddy", nullable = true)
    var isBuddy: Boolean? = null

    // 关联字段
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "song_id")
    var song: LxMaiSongLite? = null

    fun toModel(): LxMaiDifficulty = LxMaiDifficulty().apply {
        val lite = this@LxMaiDifficultyLite
        val note = lite.notes

        type = lite.type
        difficulty = lite.difficulty
        level = lite.level
        levelValue = lite.levelValue.toInt() / 10.0
        noteDesigner = lite.noteDesigner
        version = lite.version
        notes = LxMaiNote(
            note[0], note[1], note[2], note[3], note[4], note[5]
        )
        kanji = lite.kanji
        description = lite.description
        isBuddy = lite.isBuddy
    }

    companion object {
        fun from(diff: LxMaiDifficulty, chartType: String): LxMaiDifficultyLite {
            return LxMaiDifficultyLite().apply {
                type = diff.type
                this.chartType = chartType
                difficulty = diff.difficulty
                level = diff.level
                levelValue = (diff.levelValue * 10.0).roundToInt().toShort()
                noteDesigner = diff.noteDesigner
                version = diff.version
                notes = intArrayOf(
                    diff.notes.total,
                    diff.notes.tap,
                    diff.notes.hold,
                    diff.notes.slide,
                    diff.notes.touch,
                    diff.notes.`break`,
                )
                kanji = diff.kanji
                description = diff.description
                isBuddy = diff.isBuddy
            }
        }
    }
}

