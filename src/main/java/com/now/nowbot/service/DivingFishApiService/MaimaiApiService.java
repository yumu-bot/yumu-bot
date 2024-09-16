package com.now.nowbot.service.DivingFishApiService;

import com.now.nowbot.model.JsonData.MaiBestPerformance;
import com.now.nowbot.model.JsonData.MaiRanking;
import com.now.nowbot.model.JsonData.MaiSong;
import com.now.nowbot.model.enums.MaiVersion;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;

public interface MaimaiApiService {
    MaiBestPerformance getMaimaiBest50(Long qq) throws WebClientResponseException.Forbidden, WebClientResponseException.BadGateway;

    MaiBestPerformance getMaimaiBest50(String probername) throws WebClientResponseException.Forbidden, WebClientResponseException.BadGateway;

    MaiBestPerformance getMaimaiScoreByVersion(Long qq, List<MaiVersion> version) throws WebClientResponseException.Forbidden, WebClientResponseException.BadGateway;

    MaiBestPerformance getMaimaiScoreByVersion(String probername, List<MaiVersion> version) throws WebClientResponseException.Forbidden, WebClientResponseException.BadGateway;

    default byte[] getMaimaiCover(Integer songID) {
        return getMaimaiCover(songID.longValue());
    }

    byte[] getMaimaiCover(Long songID);

    List<MaiSong> getMaimaiSongLibrary(); // 开销大，这个方法应该是每周用来存数据库的

    List<MaiRanking> getMaimaiRankLibrary(); // 开销大，这个方法应该是每周用来存数据库的

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



}
