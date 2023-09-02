package com.now.nowbot.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.now.nowbot.entity.BeatmapLite;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.*;
import com.now.nowbot.model.enums.Mod;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.model.match.Match;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.client.UnknownHttpStatusCodeException;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface OsuGetService {
    /***
     * 拼合授权链接
     * @param state QQ[+群号]
     * @return
     */
    String getOauthUrl(String state);

    /***
     * 初次得到授权令牌
     * @param binUser
     * @return
     */
    JsonNode getToken(BinUser binUser);

    /***
     * 拿到机器人访客令牌
     * @return
     */
    String getToken();

    /***
     * 刷新令牌
     * @param binUser
     * @return
     */
    JsonNode refreshToken(BinUser binUser);

    /***
     * 拿到osu id值
     * @param name
     * @return
     */
    Long getOsuId(String name);

    List<MicroUser> getFriendList(BinUser user);

    /***
     * 拿到详细的个人信息 新
     * @param user
     * @return
     */
    OsuUser getPlayerOsuInfo(BinUser user);

    OsuUser getPlayerTaikoInfo(BinUser user);

    OsuUser getPlayerCatchInfo(BinUser user);

    OsuUser getPlayerManiaInfo(BinUser user);

    OsuUser getPlayerInfo(BinUser user, OsuMode mode);

    /**
     * 通过绑定信息获得user数据 刷新osu name
     *
     * @param user
     * @return
     */
    OsuUser getPlayerInfo(BinUser user);

    /**
     * 仅通过name获取user信息
     *
     * @param userName
     * @return
     */
    OsuUser getPlayerInfo(String userName);

    OsuUser getPlayerInfo(String userName, OsuMode mode);

    /***
     * 使用本机token获取user信息
     * @param id
     * @return
     */
    OsuUser getPlayerOsuInfo(Long id);

    OsuUser getPlayerTaikoInfo(Long id);

    OsuUser getPlayerCatchInfo(Long id);

    OsuUser getPlayerManiaInfo(Long id);

    OsuUser getPlayerInfo(Long id);

    OsuUser getPlayerInfo(Long id, OsuMode mode);

    String getPlayerInfoStr(Long id, OsuMode mode);

    /***
     * 使用他人token获取user信息
     * @param id
     * @param user 绑定用户
     * @param mode
     * @return
     */
    OsuUser getPlayerInfo(Long id, BinUser user, OsuMode mode);

    String getPlayerInfoN(Long id);

    /***
     * 批量获取玩家信息 小于50
     * @param users id
     * @return
     */
    <T extends Number> JsonNode getUsers(Collection<T> users);

    /**
     * 获得某个模式的bp表
     *
     * @param user
     * @param mode
     * @param s
     * @param e
     * @return
     */
    List<Score> getBestPerformance(BinUser user, OsuMode mode, int s, int e);

    /**
     * @param id
     * @param mode
     * @param s
     * @param e
     * @return
     */
    List<Score> getBestPerformance(Long id, OsuMode mode, int s, int e);

    List<JsonNode> getBestPerformance_raw(Long id, OsuMode mode, int s, int e);

    /***
     * 获得score(最近游玩列表
     * @param user
     * @param offset
     * @param limit
     * @return
     */
    List<Score> getRecentN(BinUser user, OsuMode mode, int offset, int limit);

    /***
     * 获得成绩 不包含fail
     * @param userId
     * @param mode 模式
     * @param offset 从开始
     * @param limit 不包含本身
     * @return
     */
    List<Score> getRecentN(long userId, OsuMode mode, int offset, int limit);

    List<JsonNode> getRecentNR(long userId, OsuMode mode, String s3, int s, int e);

    List<Score> getAllRecentN(long userId, OsuMode mode, int s, int e);

    List<Score> getAllRecentN(BinUser user, OsuMode mode, int s, int e);

    List<Score> getRecentN(int userId, OsuMode mode, int s, int e);

    List<Score> getAllRecentN(int userId, OsuMode mode, int s, int e);

    BeatmapUserScore getScore(long bid, long uid, OsuMode mode);

    BeatmapUserScore getScore(long bid, long uid, OsuMode mode, Mod... mods) throws JsonProcessingException;

    BeatmapUserScore getScore(long bid, BinUser user, OsuMode mode);

    List<Score> getScoreAll(long bid, BinUser user, OsuMode mode);

    List<Score> getScoreAll(long bid, long uid, OsuMode mode);

    List<Score> getBeatmapScores(long bid, OsuMode mode);

    JsonNode getScoreR(long bid, BinUser user, OsuMode mode);

    /***
     * 下载beatmap(.osu)文件
     * @param bid 谱面id
     * @return osu文件字符串流
     */
    String getBeatMapFile(long bid) throws IOException;

    /***
     * 获取map信息
     * @param bid bid
     * @return
     */
    BeatMap getBeatMapInfo(int bid);

    BeatMap getBeatMapInfo(long bid);

    BeatMap getMapInfoFromDB(long bid);

    BeatmapLite getMapInfoLite(long bid);

    JsonNode getMapInfoR(long bid);

    /***
     * 下载replay文件 字节文件
     * @param mode
     * @param id
     * @return
     */
    byte[] getReplay(String mode, long id);

    /***
     * PP+获取
     * @param name
     * @return
     */
    @Retryable(value = {SocketTimeoutException.class, ConnectException.class, UnknownHttpStatusCodeException.class}, //超时类 SocketTimeoutException, 连接失败ConnectException, 其他未知异常UnknownHttpStatusCodeException
            maxAttempts = 5, backoff = @Backoff(delay = 5000L, random = true, multiplier = 1))
    PPPlus ppPlus(String name);

    /***
     * pp+比例
     * @param ppP
     * @return
     */
    float[] ppPlus(float[] ppP);

    /***
     * 比赛信息
     * @param mid
     * @return
     */
    Match getMatchInfo(int mid);

    Match getMatchInfo(int mid, long before);

    BeatmapDifficultyAttributes getAttributes(Long id);

    BeatmapDifficultyAttributes getAttributes(Integer id);

    BeatmapDifficultyAttributes getAttributes(Long id, Mod... mods);

    BeatmapDifficultyAttributes getAttributes(Integer id, Mod... mods);

    BeatmapDifficultyAttributes getAttributes(Long id, OsuMode osuMode, Mod... mods);

    BeatmapDifficultyAttributes getAttributes(Long id, OsuMode osuMode, int modInt);

    BeatmapDifficultyAttributes getAttributes(Long id, int modInt);

    BeatmapDifficultyAttributes getAttributes(Integer id, OsuMode osuMode, Mod... mods);

    BeatmapDifficultyAttributes getAttributes(Long id, OsuMode osuMode);

    BeatmapDifficultyAttributes getAttributes(Integer id, OsuMode osuMode);

    KudosuHistory getUserKudosu(BinUser user);

    JsonNode lookupBeatmap(String checksum, String filename, Long id);

    Search searchBeatmap(Map<String, Object> query);
    JsonNode searchBeatmapN(Map<String, Object> query);

    List<JsonNode> getUserRecentActivityN(long userId, int s, int e);
    List<ActivityEvent> getUserRecentActivity(long userId, int s, int e);

    JsonNode chatGetChannels(BinUser user);

    void downloadAllFiles(long sid) throws IOException;
}
