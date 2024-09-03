package com.now.nowbot.service.MessageServiceImpl

import com.now.nowbot.dao.BindDao
import com.now.nowbot.model.BinUser
import com.now.nowbot.model.JsonData.OsuUser
import com.now.nowbot.model.JsonData.Score
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.ppminus.PPMinus
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.AtMessage
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.MessageServiceImpl.PPMinusService.PPMinusParam
import com.now.nowbot.service.OsuApiService.OsuScoreApiService
import com.now.nowbot.service.OsuApiService.OsuUserApiService
import com.now.nowbot.throwable.ServiceException.PPMinusException
import com.now.nowbot.util.CmdUtil.checkOsuMode
import com.now.nowbot.util.CmdUtil.getMode
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.OfficialInstruction
import com.now.nowbot.util.QQMsgUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
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

    data class PPMinusParam(val isVs: Boolean, val me: OsuUser, val other: OsuUser?, val mode: OsuMode)

    enum class PPMinusStatus {
        USER,
        USER_VS,
    }

    fun parseName(name1: String?, name2: String?, binMe: BinUser, binOther: BinUser, status: PPMinusStatus): Boolean {
        if (StringUtils.hasText(name1) && StringUtils.hasText(name2)) {
            //pv 1v2
            binMe.osuID = userApiService.getOsuId(name1)
            binOther.osuID = userApiService.getOsuId(name2)

            if (Objects.isNull(binMe.osuID) || Objects.isNull(binOther.osuID)) {
                throw PPMinusException(PPMinusException.Type.PM_Me_FetchFailed)
            }

            return false
        }
        val area = if (StringUtils.hasText(name1)) {
            name1
        } else {
            name2
        }
        when (status) {
            PPMinusStatus.USER -> {
                //pm 1 or 2
                binMe.osuID = userApiService.getOsuId(area)
                if (Objects.isNull(binMe.osuID)) {
                    throw PPMinusException(PPMinusException.Type.PM_Me_FetchFailed)
                }
            }

            PPMinusStatus.USER_VS -> {
                //pv 0v1 or 0v2
                binOther.osuID = userApiService.getOsuId(area)
                if (Objects.isNull(binOther.osuID)) {
                    throw PPMinusException(PPMinusException.Type.PM_Me_FetchFailed)
                }
            }
        }


        return status == PPMinusStatus.USER_VS
    }

    override fun isHandle(event: MessageEvent, messageText: String, data: DataValue<PPMinusParam>): Boolean {
        val matcher = Instruction.PP_MINUS.matcher(messageText)
        if (!matcher.find()) return false

        val status = when (Optional.ofNullable(
            matcher.group("function")
        ).orElse("pm").trim { it <= ' ' }.lowercase(Locale.getDefault())) {
            "pm", "ppm", "pp-", "p-", "ppminus", "minus" -> PPMinusStatus.USER
            "pv", "ppmv", "pmv", "pmvs", "ppmvs", "ppminusvs", "minusvs" -> PPMinusStatus.USER_VS
            else -> throw RuntimeException("PP-：未知的类型")
        }

        val area1 = matcher.group("area1")
        val area2 = matcher.group("area2")

        val at = QQMsgUtil.getType(event.message, AtMessage::class.java)

        var binMe = BinUser()
        var binOther = BinUser()

        var isMyself = false

        // 艾特
        try {
            if (at != null) {
                when (status) {
                    PPMinusStatus.USER ->  //pm @
                        binMe = bindDao.getUserFromQQ(at.target)

                    PPMinusStatus.USER_VS -> {
                        //pv 0v@
                        binMe = bindDao.getUserFromQQ(event.sender.id, true)
                        binOther = bindDao.getUserFromQQ(at.target)
                    }
                }
            } else if (StringUtils.hasText(area1) || StringUtils.hasText(area2)) {
                isMyself = parseName(area1, area2, binMe, binOther, status)
                if (isMyself) {
                    binMe = bindDao.getUserFromQQ(event.sender.id, true)
                }
            } else {
                // pm 0
                isMyself = true
                binMe = bindDao.getUserFromQQ(event.sender.id, true)
            }
        } catch (e: WebClientResponseException) {
            throw when (e) {
                is WebClientResponseException.Unauthorized -> if (isMyself) {
                    PPMinusException(PPMinusException.Type.PM_Me_TokenExpired)
                } else {
                    PPMinusException(PPMinusException.Type.PM_Player_TokenExpired)
                }

                is WebClientResponseException.NotFound ->
                    PPMinusException(PPMinusException.Type.PM_Player_NotFound)

                else ->
                    PPMinusException(PPMinusException.Type.PM_BPList_FetchFailed)
            }
        }

        val modeObj = getMode(matcher)
        var mode: OsuMode
        // 在新人群管理群里查询，则主动认为是 osu 模式
        mode = if (event.subject.id == 695600319L && OsuMode.isDefaultOrNull(modeObj.data)) {
            OsuMode.OSU
        } else {
            checkOsuMode(modeObj, binMe.osuMode)
        }

        val isVs = (binOther.osuID != null) && binMe.osuID != binOther.osuID

        val me = userApiService.getPlayerInfo(binMe, mode)
        val other = if (isVs) userApiService.getPlayerInfo(binOther, mode) else null

        if (OsuMode.isDefaultOrNull(mode)) {
            mode = OsuMode.getMode(mode, me.currentOsuMode)
        }

        data.value = PPMinusParam(isVs, me, other, mode)

        return true
    }

    /**
     * 获取 PPM 信息重写
     *
     * @param user 玩家信息
     * @return PPM 实例
     */
    private fun getPPMinus(user: OsuUser): PPMinus {
        val BPList: List<Score>

        try {
            BPList = scoreApiService.getBestPerformance(user)
        } catch (e: WebClientResponseException) {
            log.error("PP-：最好成绩获取失败", e)
            throw PPMinusException(PPMinusException.Type.PM_BPList_FetchFailed)
        }


        if (user.statistics.playTime < 60 || user.statistics.playCount < 30) {
            throw PPMinusException(PPMinusException.Type.PM_Player_PlayTimeTooShort, user.currentOsuMode.getName())
        }

        try {
            return PPMinus.getInstance(user.currentOsuMode, user, BPList)
        } catch (e: Exception) {
            log.error("PP-：数据计算失败", e)
            throw PPMinusException(PPMinusException.Type.PM_Calculate_Error)
        }
    }

    @Throws(Throwable::class)
    override fun HandleMessage(event: MessageEvent, param: PPMinusParam) {
        val from = event.subject

        val image: ByteArray

        try {
            image = if (!param.isVs && event.subject.id == 695600319L) {
                param.getPanelGamma()
            } else {
                param.getImage()
            }
        } catch (e: Exception) {
            log.error("PP-：渲染失败：", e)
            throw PPMinusException(PPMinusException.Type.PM_Render_Error)
        }

        try {
            from.sendImage(image)
        } catch (e: Exception) {
            log.error("PP-：发送失败：", e)
            throw PPMinusException(PPMinusException.Type.PM_Send_Error)
        }
    }

    override fun accept(event: MessageEvent, messageText: String): PPMinusParam? {
        var matcher: Matcher
        val status = when {
            OfficialInstruction.PP_MINUS.matcher(messageText)
                .apply { matcher = this }.find() -> {
                PPMinusStatus.USER
            }

            OfficialInstruction.PP_MINUS_VS.matcher(messageText)
                .apply { matcher = this }.find() -> {
                PPMinusStatus.USER_VS
            }

            else -> return null
        }

        val area1 = matcher.group("area1")
        val area2 = matcher.group("area2")

        var binMe = BinUser()
        val binOther = BinUser()

        try {
            if (StringUtils.hasText(area1) || StringUtils.hasText(area2)) {
                if (parseName(area1, area2, binMe, binOther, status)) {
                    binMe = bindDao.getUserFromQQ(event.sender.id, true)
                }
            } else {
                // pm 0
                binMe = bindDao.getUserFromQQ(event.sender.id, true)
            }
        } catch (e: WebClientResponseException) {
            throw when (e) {
                is WebClientResponseException.Unauthorized ->
                    PPMinusException(PPMinusException.Type.PM_Me_TokenExpired)

                is WebClientResponseException.NotFound ->
                    PPMinusException(PPMinusException.Type.PM_Player_NotFound)

                else ->
                    PPMinusException(PPMinusException.Type.PM_BPList_FetchFailed)
            }
        }

        val modeObj = getMode(matcher)
        var mode: OsuMode = checkOsuMode(modeObj, binMe.osuMode)

        val isVs = (binOther.osuID != null) && binMe.osuID != binOther.osuID

        val other = if (isVs) userApiService.getPlayerInfo(binOther, mode) else null
        val me = userApiService.getPlayerInfo(binMe, mode)

        if (OsuMode.isDefaultOrNull(mode)) {
            mode = OsuMode.getMode(mode, me.currentOsuMode)
        }

        return PPMinusParam(isVs, me, other, mode)
    }

    override fun reply(event: MessageEvent, param: PPMinusParam): MessageChain? = QQMsgUtil.getImage(param.getImage())


    fun PPMinusParam.getImage(): ByteArray {
        val my = getPPMinus(me)

        var others: PPMinus? = null

        if (isVs) {
            others = getPPMinus(other!!)
        }
        return imageService.getVS(me, other, my, others, mode)
    }

    fun PPMinusParam.getPanelGamma(): ByteArray {
        val my = getPPMinus(me)

        return imageService.getPanelGamma(me, mode, my)
    }

    fun ImageService.getVS(
        me: OsuUser,
        other: OsuUser?,
        my: PPMinus,
        others: PPMinus?,
        mode: OsuMode
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

            val PPMClass = Class.forName("com.now.nowbot.model.ppminus.PPMinus")
            val valieFields = arrayOf(
                PPMClass.getDeclaredField("value1"),
                PPMClass.getDeclaredField("value2"),
                PPMClass.getDeclaredField("value3"),
                PPMClass.getDeclaredField("value4"),
                PPMClass.getDeclaredField("value5"),
                PPMClass.getDeclaredField("value6"),
                PPMClass.getDeclaredField("value7"),
                PPMClass.getDeclaredField("value8"),
            )
            for (i in valieFields) {
                i.isAccessible = true
                i[minus] = value
            }
        }
    }
}
