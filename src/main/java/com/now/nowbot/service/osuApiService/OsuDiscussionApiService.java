package com.now.nowbot.service.osuApiService;

import com.now.nowbot.model.jsonData.Discussion;
import com.now.nowbot.model.jsonData.DiscussionDetails;
import com.now.nowbot.service.osuApiService.impl.DiscussionApiImpl;
import org.springframework.lang.Nullable;

public interface OsuDiscussionApiService {

    default Discussion getBeatMapDiscussion(long bid) {
        return getBeatMapDiscussion(bid, null, null, 50, null, false, null, null, null);
    }

    Discussion getBeatMapDiscussion(Long bid, Long sid, @Nullable DiscussionApiImpl.BeatMapSetStatus status, Integer limit, @Nullable DiscussionDetails.MessageType[] types, @Nullable Boolean onlyResolved, Integer page, @Nullable String sort, Long uid);

    default Discussion getBeatMapSetDiscussion(long sid) {
        return getBeatMapDiscussion(null, sid, null, 50, null, false, null, null, null);
    }

    default Discussion getBeatMapDiscussion(long bid, boolean solved) {
        return getBeatMapDiscussion(bid, null, null, 50, null, solved, null, null, null);
    }

    default Discussion getBeatMapSetDiscussion(long sid, boolean solved) {
        return getBeatMapDiscussion(null, sid, null, 50, null, solved, null, null, null);
    }

    default Discussion getBeatMapDiscussion(long bid, DiscussionDetails.MessageType[] types) {
        return getBeatMapDiscussion(bid, null, null, 50, types, false, null, null, null);
    }

    default Discussion getBeatMapSetDiscussion(long sid, DiscussionDetails.MessageType[] types) {
        return getBeatMapDiscussion(null, sid, null, 50, types, false, null, null, null);
    }

}
