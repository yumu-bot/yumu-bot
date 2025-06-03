package com.now.nowbot.service.osuApiService

import com.now.nowbot.model.osu.Discussion
import com.now.nowbot.service.osuApiService.impl.DiscussionApiImpl.BeatMapSetStatus

interface OsuDiscussionApiService {

    fun getBeatMapDiscussion(
        bid: Long?,
        sid: Long?,
        status: BeatMapSetStatus?,
        limit: Int? = 50,
        types: List<String>?,
        onlyResolved: Boolean?,
        page: Int?,
        sort: String?,
        uid: Long?
    ): Discussion

    fun getBeatMapDiscussion(bid: Long): Discussion {
        return getBeatMapDiscussion(bid, null, null, 50, null, false, null, null, null)
    }

    fun getBeatMapSetDiscussion(sid: Long): Discussion {
        return getBeatMapDiscussion(null, sid, null, 50, null, false, null, null, null)
    }

    fun getBeatMapDiscussion(bid: Long, solved: Boolean): Discussion {
        return getBeatMapDiscussion(bid, null, null, 50, null, solved, null, null, null)
    }

    fun getBeatMapSetDiscussion(sid: Long, solved: Boolean): Discussion {
        return getBeatMapDiscussion(null, sid, null, 50, null, solved, null, null, null)
    }

    fun getBeatMapDiscussion(bid: Long, types: List<String>?): Discussion {
        return getBeatMapDiscussion(bid, null, null, 50, types, false, null, null, null)
    }

    fun getBeatMapSetDiscussion(sid: Long, types: List<String>?): Discussion {
        return getBeatMapDiscussion(null, sid, null, 50, types, false, null, null, null)
    }
}
