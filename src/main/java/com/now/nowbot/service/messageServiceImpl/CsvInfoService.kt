package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.enums.OsuMode.Companion.getMode
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.CsvInfoService.CsvInfoParam
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.throwable.botException.CsvInfoException
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
        param: DataValue<CsvInfoParam>
    ): Boolean {
        val matcher = Instruction.CSV_INFO.matcher(messageText)
        if (!matcher.find()) return false

        val mode = getMode(matcher.group("mode"))
        val names = splitString(matcher.group("data")) ?: throw GeneralTipsException(GeneralTipsException.Type.G_Null_UserName)
        val name = if (names.isEmpty()) throw GeneralTipsException(GeneralTipsException.Type.G_Null_UserName) else "CI-" + names.first() + ".csv"

        param.value = CsvInfoParam(mode, names, name)
        return true
    }

    @Throws(Throwable::class) override fun HandleMessage(event: MessageEvent, param: CsvInfoParam) {
        if (param.users.size >= 50) event.reply(GeneralTipsException(GeneralTipsException.Type.G_Malfunction_APITooMany))

        //主获取
        val sb = StringBuilder("username,id,PP,4KPP,7KPP,accuracy,rankedScore,totalScore,playCount,playTime,totalHits,avatarUrl,countryCode,defaultGroup,isActive,isBot,isDeleted,isOnline,isSupporter,isRestricted,lastVisit,pmFriendsOnly,profileColor,coverUrl,discord,hasSupported,interests,joinDate,location,maxBlocks,maxFriends,occupation,playMode,playStyle,postCount,profileOrder,title,titleUrl,twitter,website,country.data,cover.custom,kudosu.total,beatmapPlaycount,CommentsCount,favoriteCount,followerCount,graveyardCount,guestCount,lovedCount,mappingFollowerCount,nominatedCount,pendingCount,previousNames,highestRank,rankedCount,replaysWatchedCounts,scoreBestCount,scoreFirstCount,scorePinnedCount,scoreRecentCount,supportLevel,userAchievements")

        var fetchUserFail = 0

        for (s in param.users) {
            try {
                val o = userApiService.getOsuUser(s, param.mode)
                sb.append('\n').append(o.toCSV())
            } catch (e: WebClientResponseException.TooManyRequests) {
                fetchUserFail++

                if (fetchUserFail > 3) {
                    log.error("玩家信息表：查询次数超限", e)
                    throw CsvInfoException(CsvInfoException.Type.CI_Fetch_TooManyRequest, s)
                }

                if (event.subject != null) {
                    event.reply(CsvInfoException.Type.CI_Fetch_ReachThreshold.message)
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
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(CsvInfoService::class.java)
    }
}
