package com.now.nowbot.service.osuApiService.impl;

import com.now.nowbot.model.json.Discussion;
import com.now.nowbot.model.json.DiscussionDetails;
import com.now.nowbot.service.osuApiService.OsuDiscussionApiService;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@Service
public class DiscussionApiImpl implements OsuDiscussionApiService {
    OsuApiBaseService base;

    public DiscussionApiImpl(OsuApiBaseService baseService) {
        base = baseService;
    }

    //sort默认id_desc，即最新的在前。也可以是 id_asc
    @Override
    public Discussion getBeatMapDiscussion(
            @Nullable
            Long bid,
            @Nullable
            Long sid,
            @Nullable
            BeatMapSetStatus status,
            Integer limit,
            @Nullable
            DiscussionDetails.MessageType[] types,
            @Nullable
            Boolean onlyResolved,
            @Nullable
            Integer page,
            @Nullable
            String sort,
            @Nullable
            Long uid
    ) {
        int count = 0;
        Discussion discussion =
                getBeatMapDiscussion(bid, sid, status, limit, types, onlyResolved, page, sort, uid, null);
        while (Objects.nonNull(discussion.getCursorString()) && count++ < 10) {
            var other = getBeatMapDiscussion(bid, sid, status, limit, types, onlyResolved, page, sort, uid, discussion.getCursorString());
            discussion.setCursorString(other.getCursorString());
            discussion.mergeDiscussion(other, sort);
        }
        return discussion;
    }

    public Discussion getBeatMapDiscussion(
            Long bid,
            Long sid,
            BeatMapSetStatus status,
            Integer limit,
            DiscussionDetails.MessageType[] types,
            Boolean onlyResolved,
            Integer page,
            String sort,
            Long uid,
            String cursor
    ) {
        return base.request(client -> client
                .get()
                .uri(u -> u.path("beatmapsets/discussions")
                        .queryParamIfPresent("beatmap_id", Optional.ofNullable(bid))
                        .queryParamIfPresent("beatmapset_id", Optional.ofNullable(sid))
                        .queryParamIfPresent("beatmapset_status", Optional.ofNullable(status))
                        .queryParamIfPresent("limit", Optional.ofNullable(limit))
                        .queryParamIfPresent("message_types[]", Optional.ofNullable(types))
                        .queryParamIfPresent("only_resolved", Optional.ofNullable(onlyResolved))
                        .queryParamIfPresent("page", Optional.ofNullable(page))
                        .queryParamIfPresent("sort", Optional.ofNullable(sort))
                        .queryParamIfPresent("user", Optional.ofNullable(uid))
                        .queryParamIfPresent("cursor_string", Optional.ofNullable(cursor))
                        .build())
                .headers(base::insertHeader)
                .retrieve()
                .bodyToMono(Discussion.class)
        );
    }

    public enum BeatMapSetStatus {
        all, ranked, qualified, disqualified, never_qualified,
    }
}

