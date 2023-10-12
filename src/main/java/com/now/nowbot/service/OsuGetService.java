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
import org.springframework.web.client.HttpClientErrorException;
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
    JsonNode getToken(BinUser binUser) throws HttpClientErrorException;

    /***
     * 拿到机器人访客令牌
     * @return
     */
    String getToken() throws HttpClientErrorException;

    /***
     * 刷新令牌
     * @param binUser
     * @return
     */
    JsonNode refreshToken(BinUser binUser) throws HttpClientErrorException;

    /***
     * 拿到osu id值
     * @param name
     * @return
     */
    Long getOsuId(String name) throws HttpClientErrorException;

    List<MicroUser> getFriendList(BinUser user) throws HttpClientErrorException;

    /***
     * 拿到详细的个人信息 新
     * @param user
     * @return
     */
    OsuUser getPlayerOsuInfo(BinUser user) throws HttpClientErrorException;

    OsuUser getPlayerTaikoInfo(BinUser user) throws HttpClientErrorException;

    OsuUser getPlayerCatchInfo(BinUser user) throws HttpClientErrorException;

    OsuUser getPlayerManiaInfo(BinUser user) throws HttpClientErrorException;

    OsuUser getPlayerInfo(BinUser user, OsuMode mode) throws HttpClientErrorException;

    /**
     * 通过绑定信息获得user数据 刷新osu name
     *
     * @param user
     * @return
     */
    OsuUser getPlayerInfo(BinUser user) throws HttpClientErrorException;

    /**
     * 仅通过name获取user信息
     *
     * @param userName
     * @return
     */
    OsuUser getPlayerInfo(String userName) throws HttpClientErrorException;

    OsuUser getPlayerInfo(String userName, OsuMode mode) throws HttpClientErrorException;

    /***
     * 使用本机token获取user信息
     * @param id
     * @return
     */
    OsuUser getPlayerOsuInfo(Long id) throws HttpClientErrorException;

    OsuUser getPlayerTaikoInfo(Long id) throws HttpClientErrorException;

    OsuUser getPlayerCatchInfo(Long id) throws HttpClientErrorException;

    OsuUser getPlayerManiaInfo(Long id) throws HttpClientErrorException;

    OsuUser getPlayerInfo(Long id) throws HttpClientErrorException;

    OsuUser getPlayerInfo(Long id, OsuMode mode) throws HttpClientErrorException;

    String getPlayerInfoStr(Long id, OsuMode mode) throws HttpClientErrorException;

    /***
     * 使用他人token获取user信息
     * @param id
     * @param user 绑定用户
     * @param mode
     * @return
     */
    OsuUser getPlayerInfo(Long id, BinUser user, OsuMode mode) throws HttpClientErrorException;

    String getPlayerInfoN(Long id) throws HttpClientErrorException;

    /***
     * 批量获取玩家信息 小于50
     * @param users id
     * @return
     */
    <T extends Number> List<MicroUser> getUsers(Collection<T> users) throws HttpClientErrorException;

    /**
     * 获得某个模式的bp表
     *
     * @param user
     * @param mode
     * @param s
     * @param e
     * @return
     */
    List<Score> getBestPerformance(BinUser user, OsuMode mode, int s, int e) throws HttpClientErrorException;

    /**
     * @param id
     * @param mode
     * @param s
     * @param e
     * @return
     */
    List<Score> getBestPerformance(Long id, OsuMode mode, int s, int e) throws HttpClientErrorException;

    List<JsonNode> getBestPerformance_raw(Long id, OsuMode mode, int s, int e) throws HttpClientErrorException;

    /***
     * 获得score(最近游玩列表
     * @param user
     * @param offset
     * @param limit
     * @return
     */
    List<Score> getRecentN(BinUser user, OsuMode mode, int offset, int limit) throws HttpClientErrorException;

    /***
     * 获得成绩 不包含fail
     * @param userId
     * @param mode 模式
     * @param offset 从开始
     * @param limit 不包含本身
     * @return
     */
    List<Score> getRecentN(long userId, OsuMode mode, int offset, int limit) throws HttpClientErrorException;

    List<JsonNode> getRecentNR(long userId, OsuMode mode, String s3, int s, int e) throws HttpClientErrorException;

    List<Score> getAllRecentN(long userId, OsuMode mode, int s, int e) throws HttpClientErrorException;

    List<Score> getAllRecentN(BinUser user, OsuMode mode, int s, int e) throws HttpClientErrorException;

    List<Score> getRecentN(int userId, OsuMode mode, int s, int e) throws HttpClientErrorException;

    List<Score> getAllRecentN(int userId, OsuMode mode, int s, int e) throws HttpClientErrorException;

    BeatmapUserScore getScore(long bid, long uid, OsuMode mode) throws HttpClientErrorException;

    BeatmapUserScore getScore(long bid, long uid, OsuMode mode, Mod... mods) throws JsonProcessingException;

    BeatmapUserScore getScore(long bid, BinUser user, OsuMode mode) throws HttpClientErrorException;

    List<Score> getScoreAll(long bid, BinUser user, OsuMode mode) throws HttpClientErrorException;

    List<Score> getScoreAll(long bid, long uid, OsuMode mode) throws HttpClientErrorException;

    List<Score> getBeatmapScores(long bid, OsuMode mode) throws HttpClientErrorException;

    JsonNode getScoreR(long bid, BinUser user, OsuMode mode) throws HttpClientErrorException;

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
    BeatMap getBeatMapInfo(int bid) throws HttpClientErrorException;

    BeatMap getBeatMapInfo(long bid) throws HttpClientErrorException;

    BeatMap getMapInfoFromDB(long bid) throws HttpClientErrorException;

    BeatmapLite getMapInfoLite(long bid) throws HttpClientErrorException;

    JsonNode getMapInfoR(long bid) throws HttpClientErrorException;

    /***
     * 下载replay文件 字节文件
     * @param mode
     * @param id
     * @return
     */
    byte[] getReplay(String mode, long id) throws HttpClientErrorException;

    /***
     * PP+获取
     * @param name
     * @return
     */
    @Retryable(retryFor = {SocketTimeoutException.class, ConnectException.class, UnknownHttpStatusCodeException.class}, //超时类 SocketTimeoutException, 连接失败ConnectException, 其他未知异常UnknownHttpStatusCodeException
            maxAttempts = 5, backoff = @Backoff(delay = 5000L, random = true, multiplier = 1))
    PPPlus ppPlus(String name) throws HttpClientErrorException;

    /***
     * pp+比例
     * @param ppP
     * @return
     */
    float[] ppPlus(float[] ppP) throws HttpClientErrorException;

    /***
     * 比赛信息
     * @param mid
     * @return
     */
    Match getMatchInfo(int mid) throws HttpClientErrorException;

    Match getMatchInfo(int mid, long before) throws HttpClientErrorException;

    BeatmapDifficultyAttributes getAttributes(Long id) throws HttpClientErrorException;

    BeatmapDifficultyAttributes getAttributes(Integer id) throws HttpClientErrorException;

    BeatmapDifficultyAttributes getAttributes(Long id, Mod... mods) throws HttpClientErrorException;

    BeatmapDifficultyAttributes getAttributes(Integer id, Mod... mods) throws HttpClientErrorException;

    BeatmapDifficultyAttributes getAttributes(Long id, OsuMode osuMode, Mod... mods) throws HttpClientErrorException;

    BeatmapDifficultyAttributes getAttributes(Long id, OsuMode osuMode, int modInt) throws HttpClientErrorException;

    BeatmapDifficultyAttributes getAttributes(Long id, int modInt) throws HttpClientErrorException;

    BeatmapDifficultyAttributes getAttributes(Integer id, OsuMode osuMode, Mod... mods) throws HttpClientErrorException;

    BeatmapDifficultyAttributes getAttributes(Long id, OsuMode osuMode) throws HttpClientErrorException;

    BeatmapDifficultyAttributes getAttributes(Integer id, OsuMode osuMode) throws HttpClientErrorException;

    KudosuHistory getUserKudosu(BinUser user) throws HttpClientErrorException;

    JsonNode lookupBeatmap(String checksum, String filename, Long id) throws HttpClientErrorException;

    Search searchBeatmap(Map<String, Object> query) throws HttpClientErrorException;
    JsonNode searchBeatmapN(Map<String, Object> query) throws HttpClientErrorException;

    List<JsonNode> getUserRecentActivityN(long userId, int s, int e) throws HttpClientErrorException;
    List<ActivityEvent> getUserRecentActivity(long userId, int s, int e) throws HttpClientErrorException;

    JsonNode chatGetChannels(BinUser user);

    boolean downloadAllFiles(long sid) throws IOException;
}
