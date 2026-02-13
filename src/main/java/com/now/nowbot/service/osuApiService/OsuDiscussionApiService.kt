package com.now.nowbot.service.osuApiService

import com.now.nowbot.model.osu.Discussion
import com.now.nowbot.service.osuApiService.impl.DiscussionApiImpl.BeatmapSetStatus

interface OsuDiscussionApiService {

    fun getBeatmapDiscussion(
        bid: Long?,
        sid: Long?,
        status: BeatmapSetStatus?,
        limit: Int? = 50,
        types: List<String>?,
        onlyResolved: Boolean?,
        page: Int?,
        sort: String?,
        uid: Long?
    ): Discussion

    fun getBeatmapDiscussion(bid: Long): Discussion {
        return getBeatmapDiscussion(bid, null, null, 50, null, false, null, null, null)
    }

    fun getBeatmapsetDiscussion(sid: Long): Discussion {
        return getBeatmapDiscussion(null, sid, null, 50, null, false, null, null, null)
    }

    fun getBeatmapDiscussion(bid: Long, solved: Boolean): Discussion {
        return getBeatmapDiscussion(bid, null, null, 50, null, solved, null, null, null)
    }

    fun getBeatmapsetDiscussion(sid: Long, solved: Boolean): Discussion {
        return getBeatmapDiscussion(null, sid, null, 50, null, solved, null, null, null)
    }

    fun getBeatmapDiscussion(bid: Long, types: List<String>?): Discussion {
        return getBeatmapDiscussion(bid, null, null, 50, types, false, null, null, null)
    }

    fun getBeatmapsetDiscussion(sid: Long, types: List<String>?): Discussion {
        return getBeatmapDiscussion(null, sid, null, 50, types, false, null, null, null)
    }
}
