package com.now.nowbot.dao;

import com.now.nowbot.entity.BeatmapLite;
import com.now.nowbot.entity.MapSetLite;
import com.now.nowbot.mapper.BeatMapMapper;
import com.now.nowbot.mapper.MapSetMapper;
import com.now.nowbot.model.JsonData.BeatMap;
import com.now.nowbot.model.JsonData.BeatMapSet;
import com.now.nowbot.model.JsonData.Covers;
import com.now.nowbot.service.OsuGetService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BeatMapDao {
    MapSetMapper mapSetMapper;
    BeatMapMapper beatMapMapper;
    OsuGetService osuGetService;
    @Autowired
    public BeatMapDao(MapSetMapper mapSetMapper, BeatMapMapper beatMapMapper, OsuGetService osuGetService){
        this.beatMapMapper = beatMapMapper;
        this.mapSetMapper = mapSetMapper;
        this.osuGetService = osuGetService;
    }

    public BeatmapLite saveMap(BeatMap beatMap){
        var mapSet = beatMap.getBeatMapSet();
        if (mapSet != null) {
            mapSetMapper.save(fromMapSetModel(mapSet));
        }
        return beatMapMapper.save(fromBeatmapModel(beatMap));
    }

    public BeatmapLite getBeatMapLite(int id){
        return getBeatMapLite((long) id);
    }
    public BeatmapLite getBeatMapLite(long id){
        var lite = beatMapMapper.findById(id);
        if (lite.isEmpty()) {
            var map = osuGetService.getMapInfo(id);
            return saveMap(map);
        }
        return lite.get();
    }

    public static BeatMap fromBeatmapLite(BeatmapLite map){
        var s = new BeatMap();
        BeanUtils.copyProperties(map, s);
        return s;
    }

    public static BeatmapLite fromBeatmapModel(BeatMap map){
        var s = new BeatmapLite();
        BeanUtils.copyProperties(map, s);
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
