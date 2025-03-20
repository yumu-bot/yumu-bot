package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.dao.BindDao
import com.now.nowbot.model.BindUser
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.json.LazerScore
import com.now.nowbot.model.json.OsuUser
import com.now.nowbot.model.ppminus.PPMinus
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.PPMinusService.PPMinusParam
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.TipsException
import com.now.nowbot.throwable.serviceException.PPMinusException
import com.now.nowbot.util.CmdUtil.getMode
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.OfficialInstruction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.util.*
import java.util.regex.Matcher

@Service("PP_MINUS")
class PPMinusService(
    private val userApiService: OsuUserApiService,
    private val scoreApiService: OsuScoreApiService,
    private val bindDao: BindDao,
    private val imageService: ImageService,
) : MessageService<PPMinusParam>, TencentMessageService<PPMinusParam> {

    private val newbieGroup: Long = 695600319L

    data class PPMinusParam(
        val isVs: Boolean,
        val me: OsuUser,
        val other: OsuUser?,
        val mode: OsuMode,
    )

    enum class PPMinusStatus {
        USER,
        USER_VS,
    }

    fun parseName(
        name1: String,
        name2: String,
        binMe: BindUser,
        binOther: BindUser,
        status: PPMinusStatus,
    ): Boolean {
        try {
            if (name1.isNotBlank() && name2.isNotBlank()) {
                // pv 1v2
                binMe.osuID = userApiService.getOsuId(name1)
                binOther.osuID = userApiService.getOsuId(name2)

                return false
            }
            val area = name1.ifBlank { name2 }
            when (status) {
                PPMinusStatus.USER -> {
                    // pm 1 or 2
                    binMe.osuID = userApiService.getOsuId(area)
                }

                PPMinusStatus.USER_VS -> {
                    // pv 0v1 or 0v2
                    binOther.osuID = userApiService.getOsuId(area)
                }
            }

            return status == PPMinusStatus.USER_VS
        } catch (e: Exception) {
            throw PPMinusException(PPMinusException.Type.PM_Me_FetchFailed)
        }
    }

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: DataValue<PPMinusParam>,
    ): Boolean {
        val matcher = Instruction.PP_MINUS.matcher(messageText)
        if (!matcher.find()) return false

        val status =
            when ((matcher.group("function") ?: "pm").trim().lowercase(Locale.getDefault())
            ) {
                "pm",
                "ppm",
                "pp-",
                "p-",
                "ppminus",
                "minus" -> PPMinusStatus.USER

                "pv",
                "ppmv",
                "pmv",
                "pmvs",
                "ppmvs",
                "ppminusvs",
                "minusvs" -> PPMinusStatus.USER_VS

                else -> throw RuntimeException("PP-：未知的类型")
            }

        val area1 = matcher.group("area1") ?: ""
        val area2 = matcher.group("area2") ?: ""

        var binMe = BindUser()
        var binOther = BindUser()

        var isMyself = false

        // 艾特
        try {
            if (event.isAt) {
                when (status) {
                    PPMinusStatus.USER -> // pm @
                        binMe = bindDao.getBindFromQQ(event.target)

                    PPMinusStatus.USER_VS -> {
                        // pv 0v@
                        binMe = bindDao.getBindFromQQ(event.sender.id, true)
                        binOther = bindDao.getBindFromQQ(event.target)
                    }
                }
            } else if (area1.isNotBlank() || area2.isNotBlank()) {
                isMyself = parseName(area1, area2, binMe, binOther, status)
                if (isMyself) {
                    binMe = bindDao.getBindFromQQ(event.sender.id, true)
                }
            } else {
                // pm 0
                isMyself = true
                binMe = bindDao.getBindFromQQ(event.sender.id, true)
            }
        } catch (e: WebClientResponseException) {
            throw when (e) {
                is WebClientResponseException.Unauthorized ->
                    if (isMyself) {
                        PPMinusException(PPMinusException.Type.PM_Me_TokenExpired)
                    } else {
                        PPMinusException(PPMinusException.Type.PM_Player_TokenExpired)
                    }

                is WebClientResponseException.NotFound ->
                    PPMinusException(PPMinusException.Type.PM_Player_NotFound)

                else -> PPMinusException(PPMinusException.Type.PM_BPList_FetchFailed)
            }
        }

        val modeObj = getMode(matcher)
        // 在新人群管理群里查询，则主动认为是 osu 模式
        val mode: OsuMode =
            if (event.subject.id == newbieGroup && OsuMode.isDefaultOrNull(modeObj.data)) {
                OsuMode.OSU
            } else {
                if (OsuMode.isDefaultOrNull(modeObj.data) && OsuMode.isNotDefaultOrNull(binMe.osuMode)) {
                    modeObj.data = binMe.osuMode
                    binMe.osuMode
                } else {
                    OsuMode.DEFAULT
                }
            }

        val isVs = (binOther.osuID != null) && binMe.osuID != binOther.osuID

        val me = userApiService.getPlayerInfo(binMe, mode)
        val other = if (isVs) userApiService.getPlayerInfo(binOther, mode) else null

        data.value = PPMinusParam(isVs, me, other, OsuMode.getMode(mode, me.currentOsuMode))

        return true
    }

    /**
     * 获取 PPM 信息重写
     *
     * @param user 玩家信息
     * @return PPM 实例
     */
    @Throws(PPMinusException::class)
    private fun getPPMinus(user: OsuUser): PPMinus {
        val bests: List<LazerScore>

        try {
            bests = scoreApiService.getBestScores(user)
        } catch (e: WebClientResponseException) {
            log.error("PP-：最好成绩获取失败", e)
            throw PPMinusException(PPMinusException.Type.PM_BPList_FetchFailed)
        }

        if (user.statistics.playTime < 60 || user.statistics.playCount < 30) {
            throw PPMinusException(
                PPMinusException.Type.PM_Player_PlayTimeTooShort,
                user.currentOsuMode.fullName,
            )
        }

        try {
            return PPMinus.getInstance(user.currentOsuMode, user, bests)
        } catch (e: Exception) {
            log.error("PP-：数据计算失败", e)
            throw PPMinusException(PPMinusException.Type.PM_Calculate_Error)
        }
    }

    @Throws(Throwable::class)
    override fun HandleMessage(event: MessageEvent, param: PPMinusParam) {
        val image: ByteArray

        try {
            image =
                if (!param.isVs && event.subject.id == newbieGroup) {
                    param.getPPMSpecialImage()
                } else {
                    param.getPPMImage()
                }
        } catch (e: TipsException) {
            throw e
        } catch (e: Exception) {
            log.error("PP-：渲染失败：", e)
            throw PPMinusException(PPMinusException.Type.PM_Render_Error)
        }

        try {
            event.reply(image)
        } catch (e: Exception) {
            log.error("PP-：发送失败：", e)
            throw PPMinusException(PPMinusException.Type.PM_Send_Error)
        }
    }

    override fun accept(event: MessageEvent, messageText: String): PPMinusParam? {
        var matcher: Matcher
        val status =
            when {
                OfficialInstruction.PP_MINUS.matcher(messageText)
                    .apply { matcher = this }
                    .find() -> {
                    PPMinusStatus.USER
                }

                OfficialInstruction.PP_MINUS_VS.matcher(messageText)
                    .apply { matcher = this }
                    .find() -> {
                    PPMinusStatus.USER_VS
                }

                else -> return null
            }

        val area1 = matcher.group("area1") ?: ""
        val area2 = matcher.group("area2") ?: ""

        var binMe = BindUser()
        val binOther = BindUser()

        try {
            if (area1.isNotBlank() || area2.isNotBlank()) {
                if (parseName(area1, area2, binMe, binOther, status)) {
                    binMe = bindDao.getBindFromQQ(event.sender.id, true)
                }
            } else {
                // pm 0
                binMe = bindDao.getBindFromQQ(event.sender.id, true)
            }
        } catch (e: WebClientResponseException) {
            throw when (e) {
                is WebClientResponseException.Unauthorized ->
                    PPMinusException(PPMinusException.Type.PM_Me_TokenExpired)

                is WebClientResponseException.NotFound ->
                    PPMinusException(PPMinusException.Type.PM_Player_NotFound)

                else -> PPMinusException(PPMinusException.Type.PM_BPList_FetchFailed)
            }
        }

        val modeObj = getMode(matcher)
        val mode: OsuMode = if (OsuMode.isDefaultOrNull(modeObj.data) && OsuMode.isNotDefaultOrNull(binMe.osuMode)) {
            modeObj.data = binMe.osuMode
            binMe.osuMode
        } else {
            OsuMode.DEFAULT
        }

        val isVs = (binOther.osuID != null) && binMe.osuID != binOther.osuID

        val other = if (isVs) userApiService.getPlayerInfo(binOther, mode) else null
        val me = userApiService.getPlayerInfo(binMe, mode)

        return PPMinusParam(isVs, me, other, mode = OsuMode.getMode(mode, me.currentOsuMode))
    }

    override fun reply(event: MessageEvent, param: PPMinusParam): MessageChain? {
        return MessageChain.MessageChainBuilder().addImage(param.getPPMImage()).build()
    }

    @Throws(PPMinusException::class)
    fun PPMinusParam.getPPMImage(): ByteArray {
        val my = getPPMinus(me)

        var others: PPMinus? = null

        if (isVs) {
            others = getPPMinus(other!!)
        }
        return imageService.getVS(me, other, my, others, mode)
    }

    @Throws(PPMinusException::class)
    fun PPMinusParam.getPPMSpecialImage(): ByteArray {
        val my = getPPMinus(me)

        return imageService.getPanelGamma(me, mode, my)
    }

    fun ImageService.getVS(
        me: OsuUser,
        other: OsuUser?,
        my: PPMinus,
        others: PPMinus?,
        mode: OsuMode,
    ): ByteArray {
        if (other != null) {
            if (other.id == 17064371L) {
                customizePerformanceMinus(others, 999.99f)
            } else if (other.id == 19673275L) {
                customizePerformanceMinus(others, 0f)
            }
        }
        return getPanelB1(me, other, my, others, mode)
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(PPMinusService::class.java)

        private fun customizePerformanceMinus(minus: PPMinus?, value: Float) {
            if (minus == null) return

            val ppmClass = Class.forName("com.now.nowbot.model.ppminus.PPMinus")
            val valueFields =
                arrayOf(
                    ppmClass.getDeclaredField("value1"),
                    ppmClass.getDeclaredField("value2"),
                    ppmClass.getDeclaredField("value3"),
                    ppmClass.getDeclaredField("value4"),
                    ppmClass.getDeclaredField("value5"),
                    ppmClass.getDeclaredField("value6"),
                    ppmClass.getDeclaredField("value7"),
                    ppmClass.getDeclaredField("value8"),
                )
            for (i in valueFields) {
                i.isAccessible = true
                i[minus] = value
            }
        }
    }
}
