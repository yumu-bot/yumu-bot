package com.now.nowbot.entity

import com.now.nowbot.model.JsonData.MaiSong
import io.hypersistence.utils.hibernate.type.array.DoubleArrayType
import io.hypersistence.utils.hibernate.type.array.IntArrayType
import io.hypersistence.utils.hibernate.type.array.StringArrayType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Transient
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
    var releaseDate: String,
    @Column(columnDefinition = "text[]")
    var version: String,
    var isNew: Boolean,
) {
    @Transient
    var charts:ArrayList<MaiChartLite>?=null

    fun toModel():MaiSong = with(MaiSong()) {
        this.songID = songID
        this.title = title
        this.type = type
        this.star = star
        this.level = level
        this.chartIDs = chartIDs
        this.info = with(MaiSong.SongInfo()) {
            title = songTitle
            artist = songArtist
            genre = songGenre
            bpm = songBPM
            releaseDate = this@MaiSongLite.releaseDate
            version = this@MaiSongLite.version
            isNew = this@MaiSongLite.isNew
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
        fun from(song: MaiSong):MaiSongLite {
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
                releaseDate = song.info.releaseDate,
                version = song.info.version,
                isNew = song.info.isNew
            )

            result.charts = song.charts.mapIndexed{i, c ->
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
    var notes:IntArray,

    @Column(columnDefinition = "text[]")
    var charter:String
){
    fun toModel():MaiSong.MaiChart = with(MaiSong.MaiChart()) {
        val dbNotes = this@MaiChartLite.notes
        if (this@MaiChartLite.notes.size == 5) {
            this.notes = MaiSong.MaiChart.MaiNote(
                dbNotes[0],
                dbNotes[1],
                dbNotes[2],
                dbNotes[3],
                dbNotes[4],
            )
        } else {
            this.notes = MaiSong.MaiChart.MaiNote(
                dbNotes[0],
                dbNotes[1],
                dbNotes[2],
                0,
                dbNotes[3],
            )
        }
        charter = this@MaiChartLite.charter
        this
    }
    companion object {
        @JvmStatic
        fun from(id:Int, chart: MaiSong.MaiChart):MaiChartLite {
            val notes = if (chart.notes.touchNote == 0) with(IntArray(4)) {
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