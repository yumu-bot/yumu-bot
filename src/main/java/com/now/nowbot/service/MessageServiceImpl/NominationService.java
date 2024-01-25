package com.now.nowbot.service.MessageServiceImpl;

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
        var sid = Long.parseLong(matcher.group("sid"));

        var data = parseData(sid);

        try {
            var image = imageService.getPanelN(data);
            from.sendImage(image);
        } catch (Exception e) {
            log.error("提名信息：发送失败", e);
            throw new NominationException(NominationException.Type.N_Send_Error);
        }
    }

    public Map<String, Object> parseData(long sid) throws NominationException {
        BeatMapSet s;
        Discussion d;
        final List<DiscussionDetails> discussions;
        final List<DiscussionDetails> hypes;
        Map<String, Object> more = new HashMap<>();

        try {
            s = osuBeatmapApiService.getBeatMapSetInfo(sid);
            s.getCreatorData().parseFull(osuUserApiService);
        } catch (WebClientResponseException.NotFound | HttpClientErrorException.NotFound e) {
            throw new NominationException(NominationException.Type.N_Map_NotFound);
        } catch (WebClientResponseException.BadGateway | WebClientResponseException.ServiceUnavailable e) {
            throw new NominationException(NominationException.Type.N_API_Unavailable);
        } catch (Exception e) {
            log.error("提名信息：谱面获取失败", e);
            throw new NominationException(NominationException.Type.N_Map_FetchFailed);
        }

        try {
            d = osuDiscussionApiService.getBeatMapSetDiscussion(sid);

            hypes = d.getDiscussions().stream().filter(i -> {
                var t = i.getMessageType();
                return t.equals(DiscussionDetails.MessageType.hype) || t.equals(DiscussionDetails.MessageType.praise);
            }).toList();

            discussions = d.getDiscussions().stream().filter(i -> {
                var t = i.getMessageType();
                return t.equals(DiscussionDetails.MessageType.problem) || t.equals(DiscussionDetails.MessageType.suggestion);
            }).toList();

        } catch (Exception e) {
            log.error("提名信息：讨论区获取失败", e);
            throw new NominationException(NominationException.Type.N_Discussion_FetchFailed);
        }

        //这一部分提供额外信息
        {
            int hostCount = 0;
            int guestCount = 0;
            int problemCount = 0;
            int suggestCount = 0;
            int notSolvedCount = 0;
            String maxSR;
            String minSR;
            int totalLength = 0;

            var ds = d.getDiscussions();

            for (var i : ds) {
                switch (i.getMessageType()) {
                    case problem -> problemCount ++;
                    case suggestion -> suggestCount ++;
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

            for (var b : bs) {
                if (Objects.equals(s.getCreatorID(), b.getMapperID())) {
                    hostCount ++;
                } else {
                    guestCount ++;
                }

                if (b.getStarRating() > maxStarRating) maxStarRating = b.getStarRating();
                if (b.getStarRating() < minStarRating) minStarRating = b.getStarRating();
            }

            var maxStarRatingInt = (int) Math.floor(maxStarRating);
            var minStarRatingInt = (int) Math.floor(minStarRating);

            maxSR = String.valueOf(maxStarRatingInt);
            minSR = String.valueOf(minStarRatingInt);

            if (maxStarRating - maxStarRatingInt >= 0.5) maxSR += '+';
            //if (minStarRating - minStarRatingInt >= 0.5) minSR += '+';

            if (bs.size() <= 1) minSR = "";

            //其他
            String[] tags = s.getTags().split(" ");
            /*
            int notResolved = Math.toIntExact(
                    discussions.stream().filter(d -> !d.getResolved() && d.getCanBeResolved()
                    ).count());

             */

            more.put("hostCount", hostCount);
            more.put("guestCount", guestCount);
            more.put("totalCount", hostCount + guestCount);
            more.put("maxSR", maxSR);
            more.put("minSR", minSR);
            more.put("totalLength", totalLength);
            more.put("tags", tags);
            more.put("problemCount", problemCount);
            more.put("suggestCount", suggestCount);
            more.put("notSolvedCount", notSolvedCount);
        }

        var n = new HashMap<String, Object>();
        n.put("beatmapset", s);
        n.put("discussion", discussions);
        n.put("hype", hypes);
        n.put("more", more);

        return n;
    }
}
