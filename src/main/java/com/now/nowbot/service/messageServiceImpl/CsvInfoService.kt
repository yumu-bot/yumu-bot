package com.now.nowbot.service.messageServiceImpl;

import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.osuApiService.OsuUserApiService;
import com.now.nowbot.throwable.serviceException.CsvInfoException;
import com.now.nowbot.util.DataUtil;
import com.now.nowbot.util.Instruction;
import jakarta.annotation.Resource;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service("CSV_INFO")
public class CsvInfoService implements MessageService<CsvInfoService.CIParam> {
    private static final Logger log = LoggerFactory.getLogger(CsvInfoService.class);
    @Resource
    OsuUserApiService userApiService;

    public record CIParam(OsuMode mode, List<String> users, String name) {}

    @Override
    public boolean isHandle(@NotNull MessageEvent event, @NotNull String messageText, @NotNull DataValue<CIParam> param) throws Throwable {
        var matcher = Instruction.CSV_INFO.matcher(messageText);
        if (! matcher.find()) return false;

        OsuMode mode = OsuMode.getMode(matcher.group("mode"));
        List<String> users = DataUtil.splitString(matcher.group("data"));
        String name = (users == null || users.isEmpty()) ? "CI.csv" : "CI-" + users.getFirst() +".csv";

        param.setValue(new CIParam(mode, users, name));
        return true;
    }

    @Override
    public void HandleMessage(@NotNull MessageEvent event, @NotNull CIParam param) throws Throwable {
        if (param.users.isEmpty()) throw new CsvInfoException(CsvInfoException.Type.CI_Instructions);
        if (param.users.size() >= 50) event.reply(CsvInfoException.Type.CI_Fetch_TooManyUser.message);
        if (param.users.size() > 200) throw new CsvInfoException(CsvInfoException.Type.CI_Fetch_Exceed);

        //主获取
        var sb = new StringBuilder("username,id,PP,4KPP,7KPP,accuracy,rankedScore,totalScore,playCount,playTime,totalHits,avatarUrl,countryCode,defaultGroup,isActive,isBot,isDeleted,isOnline,isSupporter,isRestricted,lastVisit,pmFriendsOnly,profileColor,coverUrl,discord,hasSupported,interests,joinDate,location,maxBlocks,maxFriends,occupation,playMode,playStyle,postCount,profileOrder,title,titleUrl,twitter,website,country.data,cover.custom,kudosu.total,beatmapPlaycount,CommentsCount,favoriteCount,followerCount,graveyardCount,guestCount,lovedCount,mappingFollowerCount,nominatedCount,pendingCount,previousNames,highestRank,rankedCount,replaysWatchedCounts,scoreBestCount,scoreFirstCount,scorePinnedCount,scoreRecentCount,supportLevel,userAchievements");

        int fetchUserFail = 0;

        for (var s : param.users) {
            try {
                var o = userApiService.getOsuUser(s, param.mode);
                sb.append('\n').append(o.toCSV());
            } catch (HttpClientErrorException.TooManyRequests | WebClientResponseException.TooManyRequests e) {
                fetchUserFail ++;
                if (fetchUserFail > 3) {
                    log.error("玩家信息表：查询次数超限", e);
                    throw new CsvInfoException(CsvInfoException.Type.CI_Fetch_TooManyRequest, s);
                }

                if (event.getSubject() != null) {
                    event.reply(CsvInfoException.Type.CI_Fetch_ReachThreshold.message);
                }

                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e1) {
                    log.error("玩家信息表：休眠意外中断", e1);
                    throw new CsvInfoException(CsvInfoException.Type.CI_Fetch_SleepingInterrupted, s);
                }
            } catch (HttpClientErrorException | WebClientResponseException e) {
                log.error("玩家信息表：网络因素无法获取", e);
                sb.append('\n').append(s).append(',').append(-1);
            } catch (Exception e) {
                log.error("玩家信息表：获取失败", e);
                throw new CsvInfoException(CsvInfoException.Type.CI_Player_FetchFailed, s);
            }
        }

        //必须群聊
        event.replyFileInGroup(sb.toString().getBytes(StandardCharsets.UTF_8), param.name);
    }
}
