package com.now.nowbot.dao;

import com.now.nowbot.entity.BeatmapLite;
import com.now.nowbot.entity.MapSetLite;
import com.now.nowbot.mapper.BeatMapMapper;
import com.now.nowbot.mapper.MapSetMapper;
import com.now.nowbot.model.json.BeatMap;
import com.now.nowbot.model.json.BeatMapSet;
import com.now.nowbot.model.json.Covers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

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

    public Optional<MapSetLite> getMapSetLite(long id){
        return beatMapMapper.getMapSetByBid(id);
    }

    public Optional<MapSetLite> getMapSetLite(int id){
        return getMapSetLite((long)id);
    }

    public List<BeatmapLite.BeatmapHitLengthResult> getAllBeatmapHitLength(Collection<Long> ids){
        return beatMapMapper.getBeatmapHitLength(ids);
    }

    public static BeatMap fromBeatmapLite(BeatmapLite bl){
        var b = bl.toBeatMap();
        b.setBeatMapSet(fromMapsetLite(bl.getMapSet()));
        return b;
    }

    public static BeatmapLite fromBeatmapModel(BeatMap b){
        var s = new BeatmapLite(b);
        MapSetLite mapSet = null;
        if (b.getBeatMapSet() != null) {
            mapSet = fromMapSetModel(b.getBeatMapSet());
        }
        s.setMapSet(mapSet);
        return s;
    }



    public static BeatMapSet fromMapsetLite(MapSetLite mapSet){
        var s = new BeatMapSet();
        s.setBeatMapSetID(Long.valueOf(mapSet.getId()));
        s.setCreatorID(Long.valueOf(mapSet.getMapperId()));
        s.setCreator(mapSet.getCreator());
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
        s.setId(Math.toIntExact(mapSet.getBeatMapSetID()));
        s.setCard(mapSet.getCovers().getCard2x());
        s.setCover(mapSet.getCovers().getCover2x());
        s.setList(mapSet.getCovers().getList2x());
        s.setSlimcover(mapSet.getCovers().getSlimcover2x());

        if (mapSet.getAvailability() != null) {
            s.setAvailabilityDownloadDisable(mapSet.getAvailability().downloadDisabled());
        }
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
