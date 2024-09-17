package com.now.nowbot.model.JsonData;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class MaiSong {

    @JsonProperty("id")
    Integer songID;

    // 曲名
    String title;

    // 种类，有 DX 和 SD
    String type;

    // 定数，也就是实际难度，类似于 osu 的星数
    @JsonProperty("ds")
    List<Double> star;

    // 定数的实际显示，0.6-0.9 后面会多一个 +，宴会场谱面会多一个 ?
    @JsonProperty("level")
    List<String> level;

    @JsonProperty("cids")
    List<Integer> chartIDs;

    @JsonProperty("charts")
    List<MaiChart> charts;

    @JsonProperty("basic_info")
    SongInfo info;

    public static class MaiChart {

        // 物件数量
        @JsonIgnoreProperties
        MaiNote notes;

        @JsonProperty("notes")
        void setNotes(List<Integer> list) {
            if (list.isEmpty() || list.size() < 4) {
                notes = new MaiNote(0, 0, 0, 0, 0);
            } else if (list.size() == 4) {
                notes = new MaiNote(list.getFirst(), list.get(1), list.get(2), 0, list.get(3));
            } else if (list.size() == 5) {
                notes = new MaiNote(list.getFirst(), list.get(1), list.get(2), list.get(4), list.get(3));
            }
        }

        // 谱师
        String charter;

        public record MaiNote (
            Integer tapNote,
            Integer holdNote,
            Integer slideNote,
            Integer touchNote, // 仅 DX 有
            Integer breakNote
        ) {}

        public MaiNote getNotes() {
            return notes;
        }

        public void setNotes(MaiNote notes) {
            this.notes = notes;
        }

        public String getCharter() {
            return charter;
        }

        public void setCharter(String charter) {
            this.charter = charter;
        }
    }


    public static class SongInfo {
        // 曲名
        String title;

        // 艺术家名
        String artist;

        // 歌曲分类，有东方Project，niconico & VOCALOID，其他游戏等等
        String genre;

        // 曲速，向下取整过的
        @JsonProperty("bpm")
        Integer BPM;

        // 预期解禁时间，这个默认为空字符串
        @JsonProperty("release_date")
        String releaseDate;

        // 加入 maimai 时的版本
        @JsonProperty("from")
        String version;

        // 歌曲是否为当前版本的新歌
        @JsonProperty("is_new")
        boolean isNew;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getArtist() {
            return artist;
        }

        public void setArtist(String artist) {
            this.artist = artist;
        }

        public String getGenre() {
            return genre;
        }

        public void setGenre(String genre) {
            this.genre = genre;
        }

        public Integer getBPM() {
            return BPM;
        }

        public void setBPM(Integer BPM) {
            this.BPM = BPM;
        }

        public String getReleaseDate() {
            return releaseDate;
        }

        public void setReleaseDate(String releaseDate) {
            this.releaseDate = releaseDate;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public boolean isNew() {
            return isNew;
        }

        public void setNew(boolean isNew) {
            this.isNew = isNew;
        }
    }

    public Integer getSongID() {
        return songID;
    }

    public void setSongID(Integer songID) {
        this.songID = songID;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<Double> getStar() {
        return star;
    }

    public void setStar(List<Double> star) {
        this.star = star;
    }

    public List<String> getLevel() {
        return level;
    }

    public void setLevel(List<String> level) {
        this.level = level;
    }

    public List<Integer> getChartIDs() {
        return chartIDs;
    }

    public void setChartIDs(List<Integer> chartIDs) {
        this.chartIDs = chartIDs;
    }

    public List<MaiChart> getCharts() {
        return charts;
    }

    public void setCharts(List<MaiChart> charts) {
        this.charts = charts;
    }

    public SongInfo getInfo() {
        return info;
    }

    public void setInfo(SongInfo info) {
        this.info = info;
    }
}
