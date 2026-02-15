package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.enums.OsuMode.Companion.getMode
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.CsvInfoService.CsvInfoParam
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.botException.CsvInfoException
import com.now.nowbot.throwable.botRuntimeException.IllegalArgumentException
import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.util.DataUtil.splitString
import com.now.nowbot.util.Instruction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.nio.charset.StandardCharsets

@Service("CSV_INFO")
class CsvInfoService(private val userApiService: OsuUserApiService) : MessageService<CsvInfoParam> {

    @JvmRecord
    data class CsvInfoParam(val mode: OsuMode, val users: List<String>, val name: String)

    @Throws(Throwable::class) override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: DataValue<CsvInfoParam>
    ): Boolean {
        val matcher = Instruction.CSV_INFO.matcher(messageText)
        if (!matcher.find()) return false

        val mode = getMode(matcher.group("mode"))
        val names = splitString(matcher.group("data"))

        val name = if (names.isNullOrEmpty()) {
            throw IllegalArgumentException.WrongException.PlayerName()
        } else {
            "CI-" + names.first() + ".csv"
        }

        data.value = CsvInfoParam(mode, names, name)
        return true
    }

    @Throws(Throwable::class) override fun handleMessage(event: MessageEvent, param: CsvInfoParam): ServiceCallStatistic? {
        if (param.users.size >= 50) event.reply(
            IllegalStateException.TooManyRequest("CSV"))

        //主获取
        val sb = StringBuilder("username,id,PP,4KPP,7KPP,accuracy,rankedScore,totalScore,playCount,playTime,totalHits,avatarUrl,countryCode,defaultGroup,isActive,isBot,isDeleted,isOnline,isSupporter,isRestricted,lastVisit,pmFriendsOnly,profileColor,coverUrl,discord,hasSupported,interests,joinDate,location,maxBlocks,maxFriends,occupation,playMode,playStyle,postCount,profileOrder,title,titleUrl,twitter,website,country.data,cover.custom,kudosu.total,beatmapPlaycount,CommentsCount,favoriteCount,followerCount,graveyardCount,guestCount,lovedCount,mappingFollowerCount,nominatedCount,pendingCount,previousNames,highestRank,rankedCount,replaysWatchedCounts,scoreBestCount,scoreFirstCount,scorePinnedCount,scoreRecentCount,supportLevel,userAchievements")

        var fetchUserFail = 0

        val users = ArrayList<OsuUser>(param.users.size)

        for (s in param.users) {
            try {
                val o = userApiService.getOsuUser(s, param.mode)
                users.add(o)
                sb.append('\n').append(o.toCSV())
            } catch (e: WebClientResponseException.TooManyRequests) {
                fetchUserFail++

                if (fetchUserFail > 3) {
                    log.error("玩家信息表：查询次数超限", e)
                    throw CsvInfoException(CsvInfoException.Type.CI_Fetch_TooManyRequest, s)
                }

                try {
                    Thread.sleep(10000)
                } catch (e1: InterruptedException) {
                    log.error("玩家信息表：休眠意外中断", e1)
                    throw CsvInfoException(CsvInfoException.Type.CI_Fetch_SleepingInterrupted, s)
                }
            } catch (e: HttpClientErrorException) {
                log.error("玩家信息表：网络因素无法获取", e)
                sb.append('\n').append(s).append(',').append(-1)
            } catch (e: WebClientResponseException) {
                log.error("玩家信息表：网络因素无法获取", e)
                sb.append('\n').append(s).append(',').append(-1)
            } catch (e: Exception) {
                log.error("玩家信息表：获取失败", e)
                throw CsvInfoException(CsvInfoException.Type.CI_Player_FetchFailed, s)
            }
        }

        //必须群聊
        event.replyFileInGroup(sb.toString().toByteArray(StandardCharsets.UTF_8), param.name)

        return ServiceCallStatistic.builds(event, userIDs = users.map { it.userID })
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(CsvInfoService::class.java)
    }
}
