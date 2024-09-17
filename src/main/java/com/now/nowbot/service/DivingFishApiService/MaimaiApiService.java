package com.now.nowbot.service.DivingFishApiService;

import com.now.nowbot.model.JsonData.*;
import com.now.nowbot.model.enums.MaiVersion;
import com.now.nowbot.util.JacksonUtil;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public interface MaimaiApiService {
    MaiBestPerformance getMaimaiBest50(Long qq) throws WebClientResponseException.Forbidden, WebClientResponseException.BadGateway;

    MaiBestPerformance getMaimaiBest50(String probername) throws WebClientResponseException.Forbidden, WebClientResponseException.BadGateway;

    MaiBestPerformance getMaimaiScoreByVersion(Long qq, List<MaiVersion> version) throws WebClientResponseException.Forbidden, WebClientResponseException.BadGateway;

    MaiBestPerformance getMaimaiScoreByVersion(String probername, List<MaiVersion> version) throws WebClientResponseException.Forbidden, WebClientResponseException.BadGateway;

    default byte[] getMaimaiCover(Integer songID) {
        return getMaimaiCover(songID.longValue());
    }

    // TODO 临时方案，目前是完全获取 但是最好自己维护一个可用的背景库，可以放在 V3 里，并且在里面更新
    byte[] getMaimaiCover(Long songID);


    // TODO 临时方案
    default MaiSong getMaimaiSong(Long songID, boolean test) {
        try {
            return getMaimaiSongLibrary(test).get(songID.intValue());
        } catch (IOException e) {
            return new MaiSong();
        }
    }

    // TODO 临时方案
    default Map<Integer, MaiSong> getMaimaiSongLibrary(boolean test) throws IOException {
        try {
            //var path = Path.of("D:/App2/[Projects]/yumu-bot-run/img/ExportFileV3/Maimai/data-songs.json");
            var path = Path.of("/home/spring/cache/nowbot/bg/ExportFileV3/Maimai/data-songs.json");
            var file = Files.readString(path);
            var maiSongList = JacksonUtil.parseObjectList(file, MaiSong.class);

            return Objects.requireNonNull(maiSongList).stream().collect(Collectors.toMap(MaiSong::getSongID, s -> s));
        } catch (IOException e) {
            return Objects.requireNonNull(getMaimaiSongLibrary()).stream().collect(Collectors.toMap(MaiSong::getSongID, s -> s));
        }
    }

    List<MaiSong> getMaimaiSongLibrary() throws IOException; // 开销大，这个方法应该是每周用来存数据库的

    // TODO 临时方案
    default Map<String, Integer> getMaimaiRankLibrary(boolean test) throws IOException {
        try {
            var path = Path.of("/home/spring/cache/nowbot/bg/ExportFileV3/Maimai/data-ranking.json");
            var file = Files.readString(path);
            var maiRankingList = JacksonUtil.parseObjectList(file, MaiRanking.class);

            return Objects.requireNonNull(maiRankingList).stream().collect(Collectors.toMap(MaiRanking::getName, MaiRanking::getRating));
        } catch (IOException e) {
            return Objects.requireNonNull(getMaimaiRankLibrary()).stream().collect(Collectors.toMap(MaiRanking::getName, MaiRanking::getRating));
        }
    }

    List<MaiRanking> getMaimaiRankLibrary() throws IOException; // 开销大，这个方法应该是每周用来存数据库的


    // TODO 临时方案
    default MaiFit getMaimaiFit(boolean test) throws IOException {
        try {
            // var path = Path.of("D:/App2/[Projects]/yumu-bot-run/img/ExportFileV3/Maimai/data-fit.json");
            var path = Path.of("/home/spring/cache/nowbot/bg/ExportFileV3/Maimai/data-fit.json");
            var file = Files.readString(path);

            return JacksonUtil.parseObject(file, MaiFit.class);
        } catch (IOException e) {
            return getMaimaiFit();
        }
    }

    MaiFit getMaimaiFit() throws IOException; // 开销大，这个方法应该是每周用来存数据库的

    MaiSong getMaimaiSong(Integer songID); // 查数据库

    MaiSong getMaimaiSong(String title); // 查数据库，而且如果能支持模糊查询就好了
    // 据说他们有的机器人存了一个自己的库来模糊匹配，比如

    /*
    @炒鸡菜 该曲目有以下别名：
    ID：10363
    oshama scramble!
    牛奶
    哦杀妈
    牛奶猫
    俩老婆
    丰胸奶
    喝奶
    o杀妈
    奶猫
    热辣大飞机
    🥛
    巧克力与香子兰
    哦啥马上吃让明白了
    牛乳
    大臣与左大臣
    dx奶
    dx牛奶
    dx奶猫
    凯尔希打嵯峨
    凯尔希嵯峨贴贴
    哦啥嘛
    哦啥嘛
    goushi
    dx黑白哈基米
    猫娘贴贴
     */

    // 都是用户自己添加的，然后维护者审核添加

    /*
    我怎么知道
    可以进入https://www.kdocs.cn/l/cauSVZId2ohu来自由填写歌曲别名。填写完成并在网页上操作保存后，发送【/更新别名】就能立刻同步给菌菌了。
     */

    // 这个是太鼓的

    // 以下需要从水鱼那里拿 DeveloperToken
    MaiScore getMaimaiScore(Long qq, Integer songID) throws WebClientResponseException.Forbidden, WebClientResponseException.BadGateway;

    List<MaiScore> getMaimaiScores(Long qq, List<Integer> songIDs) throws WebClientResponseException.Forbidden, WebClientResponseException.BadGateway;

    MaiScore getMaimaiScore(String probername, Integer songID) throws WebClientResponseException.Forbidden, WebClientResponseException.BadGateway;

    List<MaiScore> getMaimaiScores(String probername, List<Integer> songIDs) throws WebClientResponseException.Forbidden, WebClientResponseException.BadGateway;

    MaiBestPerformance getMaimaiBest50P(Long qq) throws WebClientResponseException.Forbidden, WebClientResponseException.BadGateway;

    MaiBestPerformance getMaimaiBest50P(String probername) throws WebClientResponseException.Forbidden, WebClientResponseException.BadGateway;

}
