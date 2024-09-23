package com.now.nowbot.service.divingFishApiService;

import com.now.nowbot.model.enums.MaiVersion;
import com.now.nowbot.model.json.MaiBestPerformance;
import com.now.nowbot.model.json.MaiFit;
import com.now.nowbot.model.json.MaiScore;
import com.now.nowbot.model.json.MaiSong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;

public interface MaimaiApiService {
    Logger log = LoggerFactory.getLogger(MaimaiApiService.class);

    MaiBestPerformance getMaimaiBest50(Long qq) throws WebClientResponseException.Forbidden, WebClientResponseException.BadGateway;

    MaiBestPerformance getMaimaiBest50(String probername) throws WebClientResponseException.Forbidden, WebClientResponseException.BadGateway;

    MaiBestPerformance getMaimaiScoreByVersion(Long qq, List<MaiVersion> version) throws WebClientResponseException.Forbidden, WebClientResponseException.BadGateway;

    MaiBestPerformance getMaimaiScoreByVersion(String probername, List<MaiVersion> version) throws WebClientResponseException.Forbidden, WebClientResponseException.BadGateway;

    default byte[] getMaimaiCover(Integer songID) {
        return getMaimaiCover(songID.longValue());
    }

    // TODO 临时方案，目前是完全获取 但是最好自己维护一个可用的背景库，可以放在 V3 里，并且在里面更新
    byte[] getMaimaiCover(Long songID);

    default MaiSong getMaimaiSong(Long songID) {
        return getMaimaiSongLibrary().get(songID.intValue());
    }

    Map<Integer, MaiSong> getMaimaiSongLibrary();

    Map<String, Integer> getMaimaiRankLibrary();

    MaiFit getMaimaiFit();

    // 开销大，这3个方法应该是每周用来存数据库的
    void updateMaimaiSongLibrary();

    void updateMaimaiRankLibrary();

    void updateMaimaiFit();

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