package com.now.nowbot.service.osuApiService.impl

import com.now.nowbot.model.osu.Discussion
import com.now.nowbot.service.osuApiService.OsuDiscussionApiService
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriBuilder
import java.util.*

@Service
class DiscussionApiImpl(var base: OsuApiBaseService) : OsuDiscussionApiService {
    //sort默认id_desc，即最新的在前。也可以是 id_asc
    override fun getBeatMapDiscussion(
        bid: Long?,
        sid: Long?,
        status: BeatMapSetStatus?,
        limit: Int?,
        types: List<String>?,
        onlyResolved: Boolean?,
        page: Int?,
        sort: String?,
        uid: Long?
    ): Discussion {
        var count = 0
        val discussion =
            getBeatMapDiscussion(bid, sid, status, limit, types, onlyResolved, page, sort, uid, null)
        while (discussion.cursorString.isNullOrEmpty().not() && count++ < 10) {
            val other = getBeatMapDiscussion(
                bid, sid, status, limit, types, onlyResolved, page, sort, uid, discussion.cursorString
            )

            discussion.cursorString = other.cursorString
            discussion.mergeDiscussion(other, sort!!)
        }
        return discussion
    }

    fun getBeatMapDiscussion(
        bid: Long?,
        sid: Long?,
        status: BeatMapSetStatus?,
        limit: Int?,
        types: List<String>?,
        onlyResolved: Boolean?,
        page: Int?,
        sort: String?,
        uid: Long?,
        cursor: String?
    ): Discussion {
        return base.request { client: WebClient ->
            client
                .get()
                .uri { u: UriBuilder ->
                    u.path("beatmapsets/discussions")
                        .queryParamIfPresent("beatmap_id", Optional.ofNullable(bid))
                        .queryParamIfPresent("beatmapset_id", Optional.ofNullable(sid))
                        .queryParamIfPresent("beatmapset_status", Optional.ofNullable(status))
                        .queryParamIfPresent("limit", Optional.ofNullable(limit))
                        .queryParamIfPresent("message_types[]", Optional.ofNullable(types?.toTypedArray()))
                        .queryParamIfPresent("only_resolved", Optional.ofNullable(onlyResolved))
                        .queryParamIfPresent("page", Optional.ofNullable(page))
                        .queryParamIfPresent("sort", Optional.ofNullable(sort))
                        .queryParamIfPresent("user", Optional.ofNullable(uid))
                        .queryParamIfPresent("cursor_string", Optional.ofNullable(cursor))
                        .build()
                }
                .headers { headers: HttpHeaders? ->
                    base.insertHeader(
                        headers!!
                    )
                }
                .retrieve()
                .bodyToMono(Discussion::class.java)
        }
    }

    enum class BeatMapSetStatus {
        all, ranked, qualified, disqualified, never_qualified,
    }
}

