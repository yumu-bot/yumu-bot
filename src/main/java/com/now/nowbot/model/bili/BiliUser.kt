package com.now.nowbot.model.bili

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.util.DataUtil
import java.net.URLDecoder
import java.time.format.DateTimeFormatterBuilder

class BiliUser {
    @JsonProperty("code")
    var code: Int = -1

    @JsonProperty("message")
    var message: String = ""

    // 无实义
    // @JsonProperty("ttl")
    //  var total: Int = 1

    // 如果为空，则请检查当前的 code
    @JsonProperty("data")
    var data: UserData? = null

    data class UserData(
        @JsonProperty("mid")
        val id: Long,

        @JsonProperty("name")
        val name: String = "",

        @JsonProperty("sex")
        val sex: String = "",

        @JsonProperty("face")
        val avatar: String = "",

        @JsonProperty("face_nft")
        val avatarNFT: Int = 0,

        @JsonProperty("face_nft_type")
        val avatarNFTType: Int = 0,

        /**
         * 个性签名
         */
        @JsonProperty("sign")
        val signature: String = "",

        @set:JsonProperty("rank")
        @get:JsonIgnoreProperties
        var rankInt: Int,

        // 等级，6 级最高
        @JsonProperty("level")
        val level: Byte,

        // 加入时间，恒为 0
        // @JsonProperty("jointime")
        // val joinTime: Byte,

        // 节操值，恒为 0
        // @JsonProperty("moral")
        // val moral: Byte,

        // 1 被禁言
        @JsonProperty("silence")
        val silence: Byte,

        // 硬币数量，一般没有登录验证就是 0
        @JsonProperty("coins")
        val coins: Float,

        @JsonProperty("fans_badge")
        val hasFansBadge: Boolean,

        @JsonProperty("fans_medal")
        val fansMetal: FansMetal,

        @JsonProperty("official")
        val official: Official,

        @JsonProperty("vip")
        val vip: VIP?,

        /**
         * 头像挂件
         */
        @JsonProperty("pendant")
        val pendant: Pendant,

        /**
         * 勋章
         */
        @JsonProperty("nameplate")
        val nameplate: Nameplate,

        /**
         * 这是干啥的？
         * @JsonProperty("user_honour_info")
         * val honor: Honor,
         */

        /**
         * 未登录恒为 false
         */
        @JsonProperty("is_followed")
        val followed: Boolean,

        /**
         * 头图
         */
        @JsonProperty("top_photo")
        val banner: String,

        /**
         * 头图 v2
         */
        @JsonProperty("top_photo_v2")
        val bannerV2: Banner,

        /**
         * 这是干啥的？
         * @JsonProperty("theme")
         * val theme: Theme,
         */

        /**
         * 系统提示，如果什么都没有，则是一个空对象
         */
        @JsonProperty("sys_notice")
        val systemNotice: SystemNotice,

        /**
         * 直播间的信息
         */
        @JsonProperty("live_room")
        val liveRoom: LiveRoom,

        /**
         * 生日, MM-dd
         */
        @JsonProperty("birthday")
        val birthday: String?,

        @JsonProperty("school")
        val school: School,

        @JsonProperty("profession")
        val profession: Profession,

        /**
         * 可能为 null
         */
        @JsonProperty("tags")
        val tags: List<String>?,

        /**
         * 这是干啥的？
         * @JsonProperty("series")
         * val series: Series,
         *
         *       "user_upgrade_status": 3,
         *       "show_upgrade_window": false
         */

        /**
         * 1 硬核会员
         */
        @JsonProperty("is_senior_member")
        val seniorMember: Byte,

        /**
         * 这是干啥的？
         *     "mcn_info": null,
         *     "gaia_res_type": 0,
         *     "gaia_data": null,
         *     "is_risk": false,
         */

        /**
         * 充电不叫 charge 叫 elec- 也是神人了
         */
        @JsonProperty("elec")
        val charge: Charge,


        /**
         * 这些都赤石去吧
         *
         * 这个是老粉计划
         *     "contract": {
         *       "is_display": false,
         *       "is_follow_display": false
         *     },
         *     "certificate_show": false,
         *     "name_render": null,
         */



        /**
         * 放百大 up 信息的
         */
        @JsonProperty("attestation")
        val attestation: Attestation,
    ) {

        @get:JsonProperty("rank")
        val rank: BiliUserRank
            get() = BiliUserRank.getBiliUserRank(rankInt)
    }

    data class FansMetal(
        @JsonProperty("show")
        val show: Boolean,

        @JsonProperty("wear")
        val wear: Boolean,

        @JsonProperty("medal")
        val medal: Medal?,
    )

    data class Medal(
        /**
         * 粉丝牌拥有者的 id，一般就是这个人
         */
        @JsonProperty("uid")
        val userID: Long,

        /**
         * 谁的粉丝牌
         */
        @JsonProperty("target_id")
        val targetID: Long,

        /**
         * 粉丝牌自己的编号
         */
        @JsonProperty("medal_id")
        val medalID: Long,

        @JsonProperty("level")
        val level: Byte,

        @JsonProperty("medal_name")
        val name: String,

        @set:JsonProperty("medal_color")
        @get:JsonIgnoreProperties
        var colorInt: Int,

        /**
         * 亲密度
         */
        @JsonProperty("intimacy")
        val intimacy: Int,

        @JsonProperty("next_intimacy")
        val nextIntimacy: Int,

        /**
         * 亲密度每日获取上限，大 up 一般为 10000，小 up 一般为 2000
         */
        @JsonProperty("day_limit")
        val intimacyLimit: Int,

        /**
         * 今日获取的亲密度，有可能为空
         */
        @JsonProperty("today_feed")
        val intimacyToday: Int = 0,

        @set:JsonProperty("medal_color_start")
        @get:JsonIgnoreProperties
        var colorStartInt: Int,

        @set:JsonProperty("medal_color_end")
        @get:JsonIgnoreProperties
        var colorEndInt: Int,

        @set:JsonProperty("medal_color_border")
        @get:JsonIgnoreProperties
        var colorBorderInt: Int,

        /**
         * 点亮状态（如果一直不互动那就是 0）
         */
        @JsonProperty("is_lighted")
        val lighted: Byte,

        @JsonProperty("light_status")
        val lightStatus: Byte,

        @JsonProperty("wearing_status")
        val wearingStatus: Byte,

        @JsonProperty("score")
        val score: Int,
    ) {
        @get:JsonProperty("color")
        val color: String
            get() = DataUtil.int2hex(colorInt)

        @get:JsonProperty("color_start")
        val colorStart: String
            get() = DataUtil.int2hex(colorStartInt)

        @get:JsonProperty("color_end")
        val colorEnd: String
            get() = DataUtil.int2hex(colorEndInt)

        @get:JsonProperty("color_border")
        val colorBorder: String
            get() = DataUtil.int2hex(colorBorderInt)
    }

    /**
     * 认证信息
     */
    data class Official(
        /**
         * 0 无
         * 1 个人认证：知名 UP
         * 2 个人认证：大 V 达人
         * 3 机构认证：企业
         * 4 机构认证：组织
         * 5 机构认证：媒体
         * 6 机构认证：政府
         * 7 个人认证：高能主播
         * 8
         * 9 个人认证：社会知名人士
         */
        @JsonProperty("role")
        val role: Byte,

        @JsonProperty("title")
        val title: String,

        @JsonProperty("desc")
        val description: String,

        @JsonProperty("type")
        val type: Byte,
    )

    /**
     * VIP
     */
    data class VIP(
        /**
         * 0：无
         * 1：月度+ 大会员
         * 2：年度+ 大会员
         */
        @JsonProperty("type")
        val type: Byte,

        @JsonProperty("status")
        val status: Byte,

        @set:JsonProperty("due_date")
        @get:JsonIgnoreProperties
        var dueDateNumber: Long,

        /**
         * 1：自动续费
         */
        @JsonProperty("vip_pay_type")
        val payType: Byte,

        // 尚不明确
        // @JsonProperty("theme_type")
        // val themeType: Byte,

        @JsonProperty("label")
        val label: VIPLabel,

        /**
         * 是否显示会员图标
         */
        @JsonProperty("avatar_subscript")
        val show: Byte,

        /**
         * 会员昵称颜色，一般为#FB7299，曾用于愚人节改变大会员配色
         */
        @JsonProperty("nickname_color")
        val nameColor: Byte,

        /**
         * 大会员角色类型
         * 1：月度大会员
         * 3：年度大会员
         * 7：十年大会员
         * 15：百年大会员
         */
        @JsonProperty("role")
        val role: Byte,

        // 大会员角标地址，一般为空字符串
        // @JsonProperty("avatar_subscript_url")
        // val avatarSubscriptUrl: String,

        @JsonProperty("tv_vip_status")
        val tvStatus: Byte,

        @JsonProperty("tv_vip_pay_type")
        val tvPayType: Byte,

        @set:JsonProperty("tv_due_date")
        @get:JsonIgnoreProperties
        var tvDueDateNumber: Long,

        @JsonProperty("avatar_icon")
        val icon: VIPIcon,

    ) {
        @get:JsonProperty("due_date")
        val dueDate: String
            get() = DateTimeFormatterBuilder().appendPattern("X").toFormatter().parse(dueDateNumber.toString()).toString()

        @get:JsonProperty("tv_due_date")
        val tvDueDate: String
            get() = DateTimeFormatterBuilder().appendPattern("X").toFormatter().parse(tvDueDateNumber.toString()).toString()
    }

    data class VIPLabel(

        // 尚不明确
        // @JsonProperty("path")
        // val path: String,

        /**
         * 大会员 年度大会员 十年大会员 百年大会员 最强绿鲤鱼
         */
        @JsonProperty("text")
        val text: String,

        /**
         * vip：大会员
         * annual_vip：年度大会员
         * ten_annual_vip：十年大会员
         * hundred_annual_vip：百年大会员
         * fools_day_hundred_annual_vip：最强绿鲤鱼
         */
        @JsonProperty("label_theme")
        val labelTheme: String,

        /**
         * 一般为 #ffffff
         */
        @JsonProperty("text_color")
        val textColor: String,

        // 尚不明确
        // @JsonProperty("bg_style")
        // val backgroundStyle: Byte,

        @JsonProperty("bg_color")
        val backgroundColor: String,

        /**
         * 未使用
         */
        @JsonProperty("border_color")
        val borderColor: String,

        /**
         * 恒为 true
         */
        @JsonProperty("use_img_label")
        val useImageForLabel: Boolean,

        /**
         * 未使用
         * img_label_uri_hans
         * img_label_uri_hant
         *
         * 繁体版，暂时不使用
         * img_label_uri_hant_static
         */

        /**
         * 简体版图片链接，供 bot 使用
         */
        @set:JsonProperty("img_label_uri_hans_static")
        @get:JsonIgnoreProperties
        var simplifiedImageUrl: String,
    ) {

        @get:JsonProperty("url")
        val url: String
            get() = simplifiedImageUrl
    }

    /**
     * 角标，比如 (大)
     */
    data class VIPIcon(
        /**
         * 应该和外面那个 B role 是一个东西
         */
        @JsonProperty("icon_type")
        val type: Byte,

        @JsonProperty("icon_resource")
        val resource: VIPIconResource,
    )

    data class VIPIconResource(
        @JsonProperty("type")
        val type: Byte,

        /**
         * 角标图片链接
         */
        @JsonProperty("url")
        val url: String,
    )

    /**
     * 头像挂件
     */
    data class Pendant(
        @JsonProperty("pid")
        val id: Int,

        /**
         * 新版头像挂件 id？
         */
        @JsonProperty("n_pid")
        val newID: Int,

        @JsonProperty("name")
        val name: String,

        @JsonProperty("image")
        val url: String,

        /**
         * 在动态头像框下，这个是 webp 动态图片
         */
        @JsonProperty("image_enhance")
        val urlWebp: String,

        /**
         * 在动态头像框下，这个是 png 逐帧序列
         */
        @JsonProperty("image_enhance_frame")
        val urlPNG: String,

        // 过期时间，恒为 0
        // @JsonProperty("expire")
        // val expire: Int,
    )

    /**
     * 勋章
     */
    data class Nameplate(
        @JsonProperty("nid")
        val id: Int,

        @JsonProperty("name")
        val name: String,

        @JsonProperty("image")
        val url: String,

        @JsonProperty("image_small")
        val urlSmall: String,

        /**
         * 等级（为什么他妈的是 str？
         */
        @JsonProperty("level")
        val level: String,

        /**
         * 获取条件
         */
        @set:JsonProperty("condition")
        @get:JsonIgnoreProperties
        var conditionStr: String,
    ) {
        @get:JsonProperty("condition")
        val condition: String
            get() = URLDecoder.decode(conditionStr, "utf-8")
    }

    /**
     * 系统级提示，比如争议性账号，纪念（已逝世）账号等
     * 5	该用户存在争议行为，已冻结其账号功能的使用	1
     * 8	该用户存在较大争议，请谨慎甄别其内容	1	28062215
     * 11	该账号涉及合约争议，暂冻结其账号功能使用。详见公告->	1
     * 16	该UP主内容存在争议，请注意甄别视频内信息	1	382534165
     * 20	请允许我们在此献上最后的告别，以此纪念其在哔哩哔哩留下的回忆与足迹。请点此查看纪念账号相关说明	2	212535360
     * 22	该账号涉及合约诉讼，封禁其账号使用
     * 24	该账号涉及合约争议，暂冻结其账号功能使用	1	291229008
     * 25	该用户涉及严重指控，暂冻结其账号功能使用	1	81447581
     * 31	该用户涉及严重指控，暂冻结其账号功能使用	1	22439273
     * 34	该用户涉及严重指控，暂冻结其账号功能使用	1	1640486775
     * 36	该账户存在争议，请谨慎甄别
     */
    data class SystemNotice(
        @JsonProperty("id")
        val id: Int?,

        /**
         * 1：普通
         * 2：纪念（已逝世）账号
         */
        @JsonProperty("notice_type")
        val type: Byte?,

        @JsonProperty("content")
        val content: String?,

        @JsonProperty("url")
        val url: String?,

        @JsonProperty("icon")
        val icon: String?,

        @JsonProperty("text_color")
        val textColor: String?,

        @JsonProperty("bg_color")
        val backgroundColor: String?,
    )

    /**
     * 写 bili API 的赤石去吧
     */
    data class LiveRoom(
        @JsonProperty("roomid")
        val id: Long,

        /**
         * 0：没有直播房间
         * 1：创建了直播房间
         */
        @JsonProperty("roomStatus")
        val roomStatus: Byte,

        /**
         * 0：下播
         * 1：上播
         */
        @JsonProperty("liveStatus")
        val liveStatus: Byte,

        /**
         * 0：未开启轮播
         * 1：开启轮播
         */
        @JsonProperty("roundStatus")
        val roundStatus: Byte,

        /**
         * 直播间
         */
        @JsonProperty("url")
        val url: String,

        /**
         * 直播标题
         */
        @JsonProperty("title")
        val title: String,

        /**
         * 直播封面
         */
        @JsonProperty("cover")
        val cover: String,

        /**
         * 未知
         */
        @JsonProperty("broadcast_type")
        val broadcastType: Byte,

        /**
         * 看过
         */
        @JsonProperty("watched_show")
        val watched: LiveWatched,
        )

    /**
     * 看过
     */
    data class LiveWatched(
        /**
         * 默认为 true
         */
        @JsonProperty("switch")
        val switch: Boolean,

        @JsonProperty("num")
        val count: Long,

        /**
         * 就是上面计数
         * @JsonProperty("text_small")
         * val textSmall: String,
         */

        /**
         * count 人看过
         */
        @JsonProperty("text_large")
        val text: String,

        /**
         * 一个眼睛的图标
         * @JsonProperty("icon")
         * val icon: String,
         *
         * @JsonProperty("icon_location")
         * val iconLocation: String,
         *
         * @JsonProperty("icon_web")
         * val iconWeb: String,
         */
    )

    data class School(@JsonProperty("name") val name: String)

    data class Profession(
        /**
         * 资质名称
         */
        @JsonProperty("name") val name: String,
        /**
         * 职位
         */
        @JsonProperty("department") val department: String,
        /**
         * 所属机构
         */
        @JsonProperty("title") val title: String,
        /**
         * 0：不显示
         * 1：显示
         */
        @JsonProperty("is_show") val show: Byte,
    )

    data class Charge(
        @JsonProperty("show_info") val info: ChargeInfo
    )

    data class ChargeInfo(
        @JsonProperty("show") val show: Boolean,
        @JsonProperty("state") val state: Byte,
        @JsonProperty("title") val title: String,
        @JsonProperty("icon") val icon: String,
        @JsonProperty("jump_url") val redirectUrl: String,
    )

    data class Banner(
        @JsonProperty("sid")
        val id: Int,

        @set:JsonProperty("l_img")
        @get:JsonIgnoreProperties
        var image: String,

        @set:JsonProperty("l_200h_img")
        @get:JsonIgnoreProperties
        var image200: String,
    ) {
        @get:JsonProperty("url")
        val url: String
            get() = image

        @get:JsonProperty("url_200")
        val url200: String
            get() = image200
    }

    /**
     * 百大 up
     */
    data class Attestation(
        @JsonProperty("type")
        val type: Byte,

        @JsonProperty("common_info")
        val common: AttestationCommon,

        @JsonProperty("splice_info")
        val splice: AttestationSplice,

        @JsonProperty("icon")
        val icon: String,

        @JsonProperty("desc")
        val description: String,
    )

    data class AttestationCommon(
        @JsonProperty("title")
        val title: String,

        @JsonProperty("prefix")
        val prefix: String,

        @JsonProperty("prefix_title")
        val full: String,
    )

    data class AttestationSplice(
        @JsonProperty("title")
        val title: String,
    )
}

