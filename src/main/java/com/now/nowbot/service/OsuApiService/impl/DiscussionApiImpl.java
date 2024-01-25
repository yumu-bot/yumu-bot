package com.now.nowbot.service.OsuApiService.impl;

import com.now.nowbot.model.JsonData.Discussion;
import com.now.nowbot.model.JsonData.DiscussionDetails;
import com.now.nowbot.service.OsuApiService.OsuDiscussionApiService;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class DiscussionApiImpl implements OsuDiscussionApiService {
    OsuApiBaseService base;

    public DiscussionApiImpl(OsuApiBaseService baseService) {
        base = baseService;
    }

    //sort默认id_desc，即最新的在前。也可以是 id_asc
    @Override
    public Discussion getBeatMapDiscussion(
            Long bid,
            Long sid,
            @Nullable
            BeatMapSetStatus status,
            int limit,
            @Nullable
            DiscussionDetails.MessageType[] types,
            @Nullable
            Boolean onlyResolved,
            int page,
            @Nullable
            String sort,
            @Nullable
            Long uid
    ) {
        AtomicInteger pageAtm = new AtomicInteger(page);
        Discussion discussion =
                getBeatMapDiscussion(bid, sid, status, limit, types, onlyResolved, page, sort, uid, null);
        while (Objects.nonNull(discussion.getCursorString()) && pageAtm.get() < 10) {
            var other =
                    getBeatMapDiscussion(bid, sid, status, limit, types, onlyResolved, pageAtm.getAndAdd(1), sort, uid, null);
            discussion.setCursorString(other.getCursorString());
            discussion.mergeDiscussion(other, sort);
        }
        return discussion;
    }

    public Discussion getBeatMapDiscussion(
            Long bid,
            Long sid,
            @Nullable
            BeatMapSetStatus status,
            int limit,
            @Nullable
            DiscussionDetails.MessageType[] types,
            @Nullable
            Boolean onlyResolved,
            int page,
            @Nullable
            String sort,
            Long uid,
            String cursor
    ) {
        return base.osuApiWebClient.get()
                .uri(u -> u.path("beatmapsets/discussions")
                        .queryParamIfPresent("beatmap_id", Optional.ofNullable(bid))
                        .queryParamIfPresent("beatmapset_id", Optional.ofNullable(sid))
                        .queryParamIfPresent("beatmapset_status", Optional.ofNullable(status))
                        .queryParam("limit", limit)
                        .queryParamIfPresent("message_types[]", Optional.ofNullable(types))
                        .queryParamIfPresent("only_resolved", Optional.ofNullable(onlyResolved))
                        .queryParam("page", page)
                        .queryParamIfPresent("sort", Optional.ofNullable(sort))
                        .queryParamIfPresent("user", Optional.ofNullable(uid))
                        .queryParamIfPresent("cursor_string", Optional.ofNullable(cursor))
                        .build())
                .headers(base::insertHeader)
                .retrieve()
                .bodyToMono(Discussion.class)
                .block();
    }

    public enum BeatMapSetStatus {
        all, ranked, qualified, disqualified, never_qualified,
    }
}

