package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.model.JsonData.BeatMap;
import com.now.nowbot.model.JsonData.BeatMapSet;
import com.now.nowbot.model.JsonData.Discussion;
import com.now.nowbot.model.JsonData.DiscussionDetails;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService;
import com.now.nowbot.service.OsuApiService.OsuDiscussionApiService;
import com.now.nowbot.service.OsuApiService.OsuUserApiService;
import com.now.nowbot.throwable.ServiceException.NominationException;
import com.now.nowbot.util.Instructions;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service("NOMINATION")
public class NominationService implements MessageService<Matcher> {
    private static final Logger log = LoggerFactory.getLogger(NominationService.class);

    @Resource
    OsuBeatmapApiService osuBeatmapApiService;
    @Resource
    OsuUserApiService osuUserApiService;
    @Resource
    OsuDiscussionApiService osuDiscussionApiService;
    @Resource
    ImageService imageService;

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<Matcher> data) throws Throwable {

        var matcher = Instructions.NOMINATION.matcher(messageText);
        if (! matcher.find()) return false;

        data.setValue(matcher);
        return true;
    }

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();
        long sid;
        boolean isSID = true;

        String mode = matcher.group("mode");

        if (Objects.nonNull(mode) && (Objects.equals(mode, "b") || Objects.equals(mode, "bid"))) {
            isSID = false;
        }

        try {
            sid = Long.parseLong(matcher.group("sid"));
        } catch (NumberFormatException e) {
            throw new NominationException(NominationException.Type.N_Instructions);
        }

        var data = parseData(sid, isSID, osuBeatmapApiService, osuDiscussionApiService, osuUserApiService);

        byte[] image;

        try {
            image = imageService.getPanelN(data);
        } catch (Exception e) {
            log.error("提名信息：渲染失败", e);
            throw new NominationException(NominationException.Type.N_Render_Failed);
        }

        try {
            from.sendImage(image);
        } catch (Exception e) {
            log.error("提名信息：发送失败", e);
            throw new NominationException(NominationException.Type.N_Send_Error);
        }
    }

    public static Map<String, Object> parseData(long sid, boolean isSID, OsuBeatmapApiService beatmapApiService, OsuDiscussionApiService discussionApiService, OsuUserApiService userApiService
    ) throws NominationException {
        BeatMapSet s;
        Discussion d;
        final List<DiscussionDetails> details;
        final List<DiscussionDetails> discussions;
        final List<DiscussionDetails> hypes;
        Map<String, Object> more = new HashMap<>();

        if (isSID) {
            try {
                s = beatmapApiService.getBeatMapSetInfo(sid);
            } catch (WebClientResponseException.NotFound | HttpClientErrorException.NotFound e) {
                try {
                    var b = beatmapApiService.getBeatMapInfoFromDataBase(sid);
                    sid = b.getSetID();
                    s = beatmapApiService.getBeatMapSetInfo(sid);
                } catch (WebClientResponseException.NotFound | HttpClientErrorException.NotFound e1) {
                    throw new NominationException(NominationException.Type.N_Map_NotFound);
                } catch (Exception e1) {
                    log.error("提名信息：谱面获取失败", e1);
                    throw new NominationException(NominationException.Type.N_Map_FetchFailed);
                }
            } catch (WebClientResponseException.BadGateway | WebClientResponseException.ServiceUnavailable e) {
                throw new NominationException(NominationException.Type.N_API_Unavailable);
            } catch (Exception e) {
                log.error("提名信息：谱面获取失败", e);
                throw new NominationException(NominationException.Type.N_Map_FetchFailed);
            }
        } else {
            try {
                var b = beatmapApiService.getBeatMapInfoFromDataBase(sid);
                sid = b.getSetID();
                s = beatmapApiService.getBeatMapSetInfo(sid);
            } catch (WebClientResponseException.NotFound | HttpClientErrorException.NotFound e) {
                throw new NominationException(NominationException.Type.N_Map_NotFound);
            } catch (WebClientResponseException.BadGateway | WebClientResponseException.ServiceUnavailable e) {
                throw new NominationException(NominationException.Type.N_API_Unavailable);
            } catch (Exception e) {
                log.error("提名信息：谱面获取失败", e);
                throw new NominationException(NominationException.Type.N_Map_FetchFailed);
            }
        }

        if (Objects.nonNull(s.getCreatorData())) {
            s.getCreatorData().parseFull(userApiService);
        }



        try {
            d = discussionApiService.getBeatMapSetDiscussion(sid);
        } catch (Exception e) {
            log.error("提名信息：讨论区获取失败", e);
            throw new NominationException(NominationException.Type.N_Discussion_FetchFailed);
        }

        //插入难度名
        if (Objects.nonNull(s.getBeatMaps())) {
            Map<Long, String> diffs = s.getBeatMaps().stream().collect(
                    Collectors.toMap(BeatMap::getBeatMapID, BeatMap::getDifficultyName)
            );

            d.addDifficulty4DiscussionDetails(diffs);
        }

        //获取 hypes 和 discussions 列表
        {
            //这两个list需要合并起来
            details = Stream.of(d.getDiscussions(), d.getIncludedDiscussions())
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .distinct()
                    .toList();

            hypes = details.stream().filter(i -> {
                var t = i.getMessageType();
                return t.equals(DiscussionDetails.MessageType.hype) || t.equals(DiscussionDetails.MessageType.praise);
            }).toList();

            var dis = details.stream().filter(i -> {
                var t = i.getMessageType();
                return t.equals(DiscussionDetails.MessageType.problem) || t.equals(DiscussionDetails.MessageType.suggestion);
            }).toList();

            discussions = Discussion.toppingUnsolvedDiscussionDetails(dis);
        }
        //这一部分提供额外信息
        {
            int hostCount = 0;
            int guestCount = 0;
            int problemCount = 0;
            int suggestCount = 0;
            int notSolvedCount = 0;
            int hypeCount = 0;
            int praiseCount = 0;
            String maxSR = "";
            String minSR = "";
            int totalLength = 0;
            List<Float> SRList = new ArrayList<>();

            for (var i : details) {
                switch (i.getMessageType()) {
                    case problem -> problemCount ++;
                    case suggestion -> suggestCount ++;
                    case hype -> hypeCount ++;
                    case praise -> praiseCount ++;
                }

                if (i.getCanBeResolved() && !i.getResolved()) {
                    notSolvedCount ++;
                }
            }

            var bs = s.getBeatMaps();

            //初始化星数
            float maxStarRating = 0;
            float minStarRating = 0;

            if (Objects.nonNull(bs) && !bs.isEmpty()) {
                var f = bs.getFirst();
                totalLength = f.getTotalLength();
                maxStarRating = f.getStarRating();
                minStarRating = maxStarRating;
            }

            if (Objects.nonNull(bs)) {
                for (var b : bs) {
                    if (Objects.equals(s.getCreatorID(), b.getMapperID())) {
                        hostCount ++;
                    } else {
                        guestCount ++;
                    }

                    if (b.getStarRating() > maxStarRating) maxStarRating = b.getStarRating();
                    if (b.getStarRating() < minStarRating) minStarRating = b.getStarRating();

                    SRList.add(b.getStarRating());
                }

                var maxStarRatingInt = (int) Math.floor(maxStarRating);
                var minStarRatingInt = (int) Math.floor(minStarRating);

                maxSR = String.valueOf(maxStarRatingInt);
                minSR = String.valueOf(minStarRatingInt);

                if (maxStarRating - maxStarRatingInt >= 0.5) maxSR += '+';
                //if (minStarRating - minStarRatingInt >= 0.5) minSR += '+';

                //单难度
                if (bs.size() <= 1) minSR = "";
            }

            //其他
            String[] tags = s.getTags().split(" ");

            more.put("hostCount", hostCount);
            more.put("guestCount", guestCount);
            more.put("totalCount", hostCount + guestCount);
            more.put("maxSR", maxSR);
            more.put("minSR", minSR);
            more.put("SRList", SRList.stream().sorted(
                    Comparator.comparingDouble(Float::doubleValue).reversed()
            ).toList());
            more.put("totalLength", totalLength);
            more.put("tags", tags);
            more.put("problemCount", problemCount);
            more.put("suggestCount", suggestCount);
            more.put("notSolvedCount", notSolvedCount);
            more.put("hypeCount", hypeCount);
            more.put("praiseCount", praiseCount);
        }

        var n = new HashMap<String, Object>();
        n.put("beatmapset", s);
        n.put("discussion", discussions);
        n.put("hype", hypes);
        n.put("more", more);
        n.put("users", d.getUsers());

        return n;
    }
}
