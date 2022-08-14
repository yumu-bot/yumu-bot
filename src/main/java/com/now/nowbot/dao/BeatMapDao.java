package com.now.nowbot.dao;

import com.now.nowbot.entity.BeatmapLite;
import com.now.nowbot.entity.MapSetLite;
import com.now.nowbot.mapper.BeatMapMapper;
import com.now.nowbot.mapper.MapSetMapper;
import com.now.nowbot.model.JsonData.BeatMap;
import com.now.nowbot.model.JsonData.BeatMapSet;
import com.now.nowbot.model.JsonData.Covers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BeatMapDao {
    MapSetMapper mapSetMapper;
    BeatMapMapper beatMapMapper;
    @Autowired
    public BeatMapDao(MapSetMapper mapSetMapper, BeatMapMapper beatMapMapper){
        this.beatMapMapper = beatMapMapper;
        this.mapSetMapper = mapSetMapper;
    }

    public void saveMap(BeatMap beatMap){
        var mapSet = beatMap.getBeatMapSet();
        if (mapSet != null) {
            mapSetMapper.save(fromMapSetModel(mapSet));
        }
        beatMapMapper.save(fromBeatmapModel(beatMap));
    }

    public static BeatMap fromBeatmapLite(BeatmapLite map){
        var s = new BeatMap();
        s.setHitLength(map.getHit_length());
        s.setTotalLength(map.getTotal_length());

        s.setCircles(map.getCount_circles());
        s.setSliders(map.getCount_sliders());
        s.setSpinners(map.getCount_spinners());

        s.setBpm(map.getBpm());
        s.setAR(map.getAr());
        s.setCS(map.getCs());
        s.setOD(map.getOd());
        s.setHP(map.getHp());

        s.setId(map.getBeatmapId());
        s.setBeatmapsetId(map.getMapsetId());
        s.setUserId(map.getUserId());
        s.setVersion(map.getVersion());

        s.setConvert(map.getConvert());
        s.setDifficultyRating(map.getDifficulty_rating());
        s.setMaxCombo(map.getMax_combo());
        s.setModeInt(map.getModeInt());
        s.setMode(map.getMode().getName());
        s.setPasscount(map.getPasscount());
        return s;
    }

    public static BeatmapLite fromBeatmapModel(BeatMap map){
        var s = new BeatmapLite();
        s.setBpm(map.getBpm());
        s.setAr(map.getAR());
        s.setCs(map.getCS());
        s.setOd(map.getOD());
        s.setHp(map.getHP());

        s.setBeatmapId(map.getId());
        s.setMapsetId(map.getBeatmapsetId());
        s.setUserId(map.getUserId());
        s.setVersion(map.getVersion());

        s.setCount_circles(map.getCircles());
        s.setCount_sliders(map.getSliders());
        s.setCount_spinners(map.getSpinners());

        s.setTotal_length(map.getTotalLength());
        s.setHit_length(map.getHitLength());
        s.setConvert(map.getConvert());
        s.setDifficulty_rating(map.getDifficultyRating());
        s.setMax_combo(map.getMaxCombo());
        s.setMode(map.getModeInt());
        s.setPasscount(map.getPasscount());

        return s;
    }



    public static BeatMapSet fromMapsetLite(MapSetLite mapSet){
        var s = new BeatMapSet();
        s.setId(mapSet.getMapset_id());
        s.setMapperId(mapSet.getUser_id());
        var cover = new Covers();
        cover.setCover(mapSet.getCover());
        cover.setCover2x(mapSet.getCover());
        cover.setCard(mapSet.getCard());
        cover.setCard2x(mapSet.getCard());
        cover.setList(mapSet.getList());
        cover.setList2x(mapSet.getList());
        cover.setSlimcover(mapSet.getSlimcover());
        cover.setSlimcover2x(mapSet.getSlimcover());
        s.setCovers(cover);

        s.setNsfw(mapSet.getNsfw());
        s.setAvailabilityDownloadDisable(mapSet.getDownload_disabled());
        s.setStoryboard(mapSet.getStoryboard());

        s.setMapperId(mapSet.getUser_id());
        s.setCreator(mapSet.getCreator());
        s.setSource(mapSet.getSource());
        s.setStatus(mapSet.getStatus());
        s.setPlayCount(mapSet.getPlayCount());
        s.setFavourite(mapSet.getFavourite_count());
        s.setTitle(mapSet.getTitle());
        s.setTitleUTF(mapSet.getTitle_unicode());
        s.setArtist(mapSet.getArtist());
        s.setArtistUTF(mapSet.getArtist_unicode());

        s.FromDatabases();
        return s;
    }

    public static MapSetLite fromMapSetModel(BeatMapSet mapSet){
        var s = new MapSetLite();
        s.setMapset_id(mapSet.getId());
        s.setCard(mapSet.getCovers().getCard2x());
        s.setCover(mapSet.getCovers().getCover2x());
        s.setList(mapSet.getCovers().getList2x());
        s.setSlimcover(mapSet.getCovers().getSlimcover2x());

        s.setDownload_disabled(mapSet.getAvailabilityDownloadDisable());
        s.setNsfw(mapSet.getNsfw());
        s.setStoryboard(mapSet.getStoryboard());

        s.setUser_id(mapSet.getMapperId());
        s.setCreator(mapSet.getCreator());
        s.setSource(mapSet.getSource());
        s.setStatus(mapSet.getStatus());
        s.setPlay_count(mapSet.getPlayCount());
        s.setFavourite_count(mapSet.getFavourite());
        s.setTitle(mapSet.getTitle());
        s.setTitle_unicode(mapSet.getTitleUTF());
        s.setArtist(mapSet.getArtist());
        s.setArtist_unicode(mapSet.getArtistUTF());

        return s;
    }


}
