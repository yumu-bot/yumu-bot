package com.now.nowbot.service.divingFishApiService

import com.now.nowbot.model.enums.MaiVersion
import com.now.nowbot.model.json.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.reactive.function.client.WebClientResponseException

interface MaimaiApiService {
    @Throws(
            WebClientResponseException.Forbidden::class,
            WebClientResponseException.BadGateway::class)
    fun getMaimaiBest50(qq: Long): MaiBestScore

    @Throws(
            WebClientResponseException.Forbidden::class,
            WebClientResponseException.BadGateway::class)
    fun getMaimaiBest50(username: String): MaiBestScore

    @Throws(
            WebClientResponseException.Forbidden::class,
            WebClientResponseException.BadGateway::class)
    fun getMaimaiScoreByVersion(qq: Long, versions: MutableList<MaiVersion>): MaiVersionScore

    @Throws(
            WebClientResponseException.Forbidden::class,
            WebClientResponseException.BadGateway::class)
    fun getMaimaiScoreByVersion(username: String, versions: MutableList<MaiVersion>): MaiVersionScore

    fun getMaimaiCover(songID: Long): ByteArray

    fun getMaimaiCoverFromAPI(songID: Long): ByteArray

    val maimaiSongLibrary: Map<Int, MaiSong>

    val maimaiRankLibrary: Map<String, Int>

    val maimaiFitLibrary: MaiFit

    fun updateMaimaiSongLibrary()

    fun updateMaimaiRankLibrary()

    fun updateMaimaiFitLibrary()

    fun getMaimaiSong(songID: Int): MaiSong // 查数据库

    fun getMaimaiSong(title: String): MaiSong // 查数据库，而且如果能支持模糊查询就好了

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
    @Throws(
            WebClientResponseException.Forbidden::class,
            WebClientResponseException.BadGateway::class)
    fun getMaimaiSongScore(qq: Long, songID: Int): MaiScore

    @Throws(
            WebClientResponseException.Forbidden::class,
            WebClientResponseException.BadGateway::class)
    fun getMaimaiSongsScore(qq: Long, songIDs: List<Int>): List<MaiScore>

    @Throws(
            WebClientResponseException.Forbidden::class,
            WebClientResponseException.BadGateway::class)
    fun getMaimaiSongScore(username: String, songID: Int): MaiScore

    @Throws(
            WebClientResponseException.Forbidden::class,
            WebClientResponseException.BadGateway::class)
    fun getMaimaiSongsScore(username: String, songIDs: List<Int>): List<MaiScore>

    @Throws(
            WebClientResponseException.Forbidden::class,
            WebClientResponseException.BadGateway::class)
    fun getMaimaiFullScores(qq: Long): MaiBestScore

    @Throws(
            WebClientResponseException.Forbidden::class,
            WebClientResponseException.BadGateway::class)
    fun getMaimaiFullScores(username: String): MaiBestScore

    fun getMaimaiPossibleSong(text : String): Map<Double, MaiSong>?

    companion object {
        val log: Logger = LoggerFactory.getLogger(MaimaiApiService::class.java)
    }
}
