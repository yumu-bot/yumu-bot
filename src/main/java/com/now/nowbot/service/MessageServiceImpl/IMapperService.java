package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.*;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService;
import com.now.nowbot.service.OsuApiService.OsuUserApiService;
import com.now.nowbot.throwable.ServiceException.IMapperException;
import com.now.nowbot.util.Instructions;
import com.now.nowbot.util.QQMsgUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;

@Service("IMAPPER")
public class IMapperService implements MessageService<Matcher> {
    private static final Logger log = LoggerFactory.getLogger(IMapperService.class);
    OsuUserApiService userApiService;
    OsuBeatmapApiService beatmapApiService;
    BindDao bindDao;
    ImageService imageService;

    @Autowired
    public IMapperService(OsuUserApiService userApiService,
                          OsuBeatmapApiService beatmapApiService,
                          BindDao bindDao, ImageService image) {
        this.userApiService = userApiService;
        this.beatmapApiService = beatmapApiService;
        this.bindDao = bindDao;
        imageService = image;
    }

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<Matcher> data) {
        var m = Instructions.IMAPPER.matcher(messageText);
        if (m.find()) {
            data.setValue(m);
            return true;
        } else return false;
    }

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();

        OsuUser osuUser;
        String name = matcher.group("name").trim();

        if (name.isEmpty()) {
            BinUser binUser;

            try {
                binUser = bindDao.getUserFromQQ(event.getSender().getId());
            } catch (Exception e) {
                throw new IMapperException(IMapperException.Type.IM_Me_TokenExpired);
            }

            try {
                osuUser = userApiService.getPlayerInfo(binUser);
            } catch (Exception e) {
                throw new IMapperException(IMapperException.Type.IM_Me_NotFound);
            }

        } else {
            try {
                osuUser = userApiService.getPlayerInfo(name);
            } catch (Exception e) {
                try {
                    var uid = Long.parseLong(matcher.group("name"));
                    osuUser = userApiService.getPlayerInfo(uid);
                } catch (Exception e1) {
                    throw new IMapperException(IMapperException.Type.IM_Player_NotFound);
                }
            }
        }
        try {
            var map = parseData(osuUser, userApiService, beatmapApiService);
            var data = imageService.getPanelM(map);

            QQMsgUtil.sendImage(from, data);
        } catch (Exception e) {
            log.error("IMapper", e);
            throw new IMapperException(IMapperException.Type.IM_Send_Error);
        }
    }

    public Map<String, Object> parseData(OsuUser user, OsuUserApiService userApiService, OsuBeatmapApiService beatmapApiService) {
        var page = 1;
        var query = new HashMap<String, Object>();
        query.put("q", "creator=" + user.getUID());
        query.put("sort", "ranked_desc");
        query.put("s", "any");
        query.put("page", page);

        Search search = null;
        //依据QualifiedMapService 的逻辑来多次获取

        {
            int resultCount = 0;
            do {
                if (Objects.isNull(search)) {
                    search = beatmapApiService.searchBeatmap(query);
                    resultCount += search.getBeatmapSets().size();
                    continue;
                }
                page++;
                query.put("page", page);
                var result = beatmapApiService.searchBeatmap(query);
                resultCount += result.getResultCount();
                search.getBeatmapSets().addAll(result.getBeatmapSets());
            } while (resultCount < search.getTotal() && page < 10);
        }

        var result = search.getBeatmapSets();

        List<ActivityEvent> activity;
        final List<ActivityEvent> mappingActivity = new ArrayList<>();

        try {
            activity = userApiService.getUserRecentActivity(user.getUID(), 0, 100);

            activity.stream()
                    .filter(ActivityEvent::isMapping)
                    .forEach(e -> {
                        if (CollectionUtils.isEmpty(mappingActivity)) {
                            mappingActivity.add(e);
                            return;
                        }
                        var last = mappingActivity.getLast();
                        if (e.equals(last)) return;
                        //if (Objects.equals(last, e)) return;
                        mappingActivity.add(e);
                    });

        } catch (Exception ignore) { }

        var mostPopularBeatmap = result
                .stream()
                .filter(s -> (Objects.equals(s.getCreatorID(), user.getUID())))
                .sorted(Comparator.comparing(BeatMapSet::getPlayCount).reversed())
                .limit(6)
                .toList();

        var mostRecentRankedBeatmap = result
                .stream()
                .filter(s -> (s.hasLeaderBoard() && Objects.equals(user.getUID(), s.getCreatorID())))
                .findFirst()
                .orElse(null);

        if (Objects.isNull(mostRecentRankedBeatmap) && user.getRankedCount() > 0) {
            try {
                var newQuery = new HashMap<String, Object>();
                newQuery.put("q", user.getUID().toString());
                newQuery.put("sort", "ranked_desc");
                newQuery.put("s", "any");
                newQuery.put("page", 1);

                var newSearch = beatmapApiService.searchBeatmap(newQuery);
                mostRecentRankedBeatmap = newSearch.getBeatmapSets().stream().filter(BeatMapSet::hasLeaderBoard).findFirst().orElse(null);
            } catch (Exception ignored) {

            }
        }

        var mostRecentRankedGuestDiff = result
                .stream()
                .filter(s -> (s.hasLeaderBoard()) && !Objects.equals(user.getUID(), s.getCreatorID()))
                .findFirst()
                .orElse(null);

        var beatMapSum = search.getBeatmapSets().stream().flatMap(s -> s.getBeatMaps().stream()).toList();

        var diffArr = new int[8];
        {
            var diffStar = beatMapSum.stream().filter(b -> Objects.equals(b.getMapperID(), user.getUID())).mapToDouble(BeatMap::getStarRating).toArray();
            var starMaxBoundary = new double[]{2f, 2.8f, 4f, 5.3f, 6.5f, 8f, 10f, Double.MAX_VALUE};
            for (var d : diffStar) {
                for (int i = 0; i < 8; i++) {
                    if (d <= starMaxBoundary[i]) {
                        diffArr[i]++;
                        break;
                    }
                }
            }
        }

        int[] genre;
        {
            String[] keywords = new String[]{"unspecified", "video game", "anime", "rock", "pop", "other", "novelty", "hip hop", "electronic", "metal", "classical", "folk", "jazz"};
            genre = new int[keywords.length];
            AtomicBoolean hasAnyGenre = new AtomicBoolean(false);

            //逻辑应该是先每张图然后再遍历12吧？
            if (!CollectionUtils.isEmpty(search.getBeatmapSets())) {
                search.getBeatmapSets().forEach(m -> {

                    for (int i = 1; i < keywords.length; i++) {
                        var keyword = keywords[i];

                        if (m.getTags().toLowerCase().contains(keyword)) {
                            genre[i]++;
                            hasAnyGenre.set(true);
                        }
                    }

                    //0是实在找不到 tag 的时候所赋予的默认值
                    if (!hasAnyGenre.get()) {
                        genre[0]++;
                    }

                    hasAnyGenre.set(false);
                });
            }
        }

        int favorite = 0;
        int playcount = 0;
        if (!CollectionUtils.isEmpty(search.getBeatmapSets())) {
            for (int i = 0; i < search.getBeatmapSets().size(); i++) {
                var v = search.getBeatmapSets().get(i);

                if (v.getCreatorID() == user.getUID().intValue()) {
                    favorite += v.getFavouriteCount();
                    playcount += v.getPlayCount();
                }
            }
        }

        var lengthArr = new int[8];
        {
            var lengthAll = beatMapSum.stream().filter(b -> b.getMapperID().longValue() == user.getUID()).mapToDouble(BeatMap::getTotalLength).toArray();
            var lengthMaxBoundary = new double[]{60, 90, 120, 150, 180, 210, 240, Double.MAX_VALUE};
            for (var f : lengthAll) {
                for (int i = 0; i < 8; i++) {
                    if (f <= lengthMaxBoundary[i]) {
                        lengthArr[i]++;
                        break;
                    }
                }
            }
        }

        var body = new HashMap<String, Object>();

        body.put("user", user);
        body.put("most_popular_beatmap", mostPopularBeatmap);
        body.put("most_recent_ranked_beatmap", mostRecentRankedBeatmap);
        body.put("most_recent_ranked_guest_diff", mostRecentRankedGuestDiff);
        body.put("difficulty_arr", diffArr);
        body.put("length_arr", lengthArr);
        body.put("genre", genre);
        body.put("recent_activity", mappingActivity);
        body.put("favorite", favorite);
        body.put("playcount", playcount);

        return body;
    }
}
