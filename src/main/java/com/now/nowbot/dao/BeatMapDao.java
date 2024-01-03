package com.now.nowbot.dao;

import com.now.nowbot.entity.BeatmapLite;
import com.now.nowbot.entity.MapSetLite;
import com.now.nowbot.mapper.BeatMapMapper;
import com.now.nowbot.mapper.MapSetMapper;
import com.now.nowbot.model.JsonData.BeatMap;
import com.now.nowbot.model.JsonData.BeatMapSet;
import com.now.nowbot.model.JsonData.Covers;
import org.springframework.beans.BeanUtils;
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

    public BeatmapLite saveMap(BeatMap beatMap){
        var mapSet = beatMap.getBeatMapSet();
        if (mapSet != null) {
            mapSetMapper.save(fromMapSetModel(mapSet));
        }
        return beatMapMapper.save(fromBeatmapModel(beatMap));
    }

    public MapSetLite saveMapSet(BeatMapSet beatMapSet){
        return mapSetMapper.save(fromMapSetModel(beatMapSet));
    }

    public BeatmapLite getBeatMapLite(int id){
        return getBeatMapLite((long) id);
    }
    public BeatmapLite getBeatMapLite(long id){
        var lite = beatMapMapper.findById(id);
        if (lite.isEmpty()) {
            throw new NullPointerException("not found");
        }
        return lite.get();
    }

    public static BeatMap fromBeatmapLite(BeatmapLite map){
        var s = new BeatMap();
        var mapSet = fromMapsetLite(map.getMapSet());
        s.setBeatMapSet(mapSet);
        BeanUtils.copyProperties(map, s);
        s.setBpm(map.getBpm());
        return s;
    }

    public static BeatmapLite fromBeatmapModel(BeatMap map){
        var s = new BeatmapLite();
        var mapSet = fromMapSetModel(map.getBeatMapSet());
        BeanUtils.copyProperties(map, s);
        s.setMapSet(mapSet);
        return s;
    }



    public static BeatMapSet fromMapsetLite(MapSetLite mapSet){
        var s = new BeatMapSet();
        s.setSID(Long.valueOf(mapSet.getId()));
        s.setCreatorID(Long.valueOf(mapSet.getMapperId()));
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
        s.setStoryboard(mapSet.getStoryboard());
        s.setSource(mapSet.getSource());
        s.setStatus(mapSet.getStatus());
        s.setPlayCount(mapSet.getPlayCount());
        s.setFavouriteCount(mapSet.getFavourite());
        s.setTitle(mapSet.getTitle());
        s.setTitleUnicode(mapSet.getTitleUTF());
        s.setArtist(mapSet.getArtist());
        s.setArtistUnicode(mapSet.getArtistUTF());
        s.setLegacyThreadUrl(mapSet.getLegacyUrl());

        s.setFromDatabase(false);
        return s;
    }

    public static MapSetLite fromMapSetModel(BeatMapSet mapSet){
        var s = new MapSetLite();
        s.setId(Math.toIntExact(mapSet.getSID()));
        s.setCard(mapSet.getCovers().getCard2x());
        s.setCover(mapSet.getCovers().getCover2x());
        s.setList(mapSet.getCovers().getList2x());
        s.setSlimcover(mapSet.getCovers().getSlimcover2x());

        s.setAvailabilityDownloadDisable(mapSet.getAvailability().downloadDisabled());
        s.setNsfw(mapSet.getNsfw());
        s.setStoryboard(mapSet.getStoryboard());
        s.setLegacyUrl(mapSet.getLegacyThreadUrl());

        s.setMapperId(Math.toIntExact(mapSet.getCreatorID()));
        s.setCreator(mapSet.getCreator());
        s.setSource(mapSet.getSource());
        s.setStatus(mapSet.getStatus());
        s.setPlayCount(mapSet.getPlayCount());
        s.setFavourite(mapSet.getFavouriteCount());
        s.setTitle(mapSet.getTitle());
        s.setTitleUTF(mapSet.getTitleUnicode());
        s.setArtist(mapSet.getArtist());
        s.setArtistUTF(mapSet.getArtistUnicode());

        return s;
    }


}
