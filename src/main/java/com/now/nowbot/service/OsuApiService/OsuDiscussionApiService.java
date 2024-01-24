package com.now.nowbot.service.OsuApiService;

import com.now.nowbot.model.JsonData.Discussion;
import com.now.nowbot.model.JsonData.DiscussionDetails;
import com.now.nowbot.service.OsuApiService.impl.DiscussionApiImpl;
import org.springframework.lang.Nullable;

public interface OsuDiscussionApiService {

    default Discussion getBeatMapDiscussion(long bid) {
        return getBeatMapDiscussion(bid, null, null, 100, null, false, 1, null, null);
    }

    default Discussion getBeatMapSetDiscussion(long sid) {
        return getBeatMapDiscussion(null, sid, null, 100, null, false, 1, null, null);
    }

    default Discussion getBeatMapDiscussion(long bid, boolean solved) {
        return getBeatMapDiscussion(bid, null, null, 100, null, solved, 1, null, null);
    }

    default Discussion getBeatMapSetDiscussion(long sid, boolean solved) {
        return getBeatMapDiscussion(null, sid, null, 100, null, solved, 1, null, null);
    }

    default Discussion getBeatMapDiscussion(long bid, DiscussionDetails.MessageType[] types) {
        return getBeatMapDiscussion(bid, null, null, 100, types, false, 1, null, null);
    }

    default Discussion getBeatMapSetDiscussion(long sid, DiscussionDetails.MessageType[] types) {
        return getBeatMapDiscussion(null, sid, null, 100, types, false, 1, null, null);
    }

    Discussion getBeatMapDiscussion(Long bid, Long sid, @Nullable DiscussionApiImpl.BeatMapSetStatus status, int limit, @Nullable DiscussionDetails.MessageType[] types, @Nullable Boolean onlyResolved, int page, @Nullable String sort, Long uid);

}
