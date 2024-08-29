package com.now.nowbot.service.MessageServiceImpl

import com.now.nowbot.dao.OsuUserInfoDao
import com.now.nowbot.entity.OsuUserInfoArchiveLite
import com.now.nowbot.model.JsonData.OsuUser
import com.now.nowbot.model.JsonData.Score
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.MessageServiceImpl.InfoService.InfoParam
import com.now.nowbot.service.OsuApiService.OsuScoreApiService
import com.now.nowbot.throwable.ServiceException.InfoException
import com.now.nowbot.throwable.TipsException
import com.now.nowbot.util.CmdUtil.getMode
import com.now.nowbot.util.CmdUtil.getUserWithOutRange
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.OfficialInstruction
import com.now.nowbot.util.QQMsgUtil
import com.now.nowbot.util.command.FLAG_DAY
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Matcher

@Service("INFO")
class InfoService(
    private val scoreApiService: OsuScoreApiService,
    private val infoDao: OsuUserInfoDao,
    private val imageService: ImageService,
) : MessageService<InfoParam>, TencentMessageService<InfoParam> {

    data class InfoParam(
        val user: OsuUser,
        val mode: OsuMode,
        val day: Int,
        val isMyself: Boolean
    )

    @Throws(TipsException::class)
    override fun isHandle(event: MessageEvent, messageText: String, data: DataValue<InfoParam>): Boolean {
        val matcher = Instruction.INFO.matcher(messageText)

        if (!matcher.find()) return false

        data.value = getParam(event, matcher)

        return true
    }

    @Throws(Throwable::class)
    override fun HandleMessage(event: MessageEvent, param: InfoParam) {
        val from = event.subject
        val image = param.getImage()
        try {
            from.sendImage(image)
        } catch (e: Exception) {
            log.error("玩家信息：发送失败", e)
            throw InfoException(InfoException.Type.I_Send_Error)
        }
    }

    override fun accept(event: MessageEvent, messageText: String): InfoParam? {
        val matcher = OfficialInstruction.INFO.matcher(messageText)

        if (!matcher.find()) return null

        return getParam(event, matcher)
    }

    override fun reply(event: MessageEvent, param: InfoParam): MessageChain? = QQMsgUtil.getImage(param.getImage())

    private fun getParam(event: MessageEvent, matcher: Matcher): InfoParam {
        val mode = getMode(matcher)

        val isMyself = AtomicBoolean()

        val user = getUserWithOutRange(event, matcher, mode, isMyself)
            ?: throw InfoException(InfoException.Type.I_Player_NotFound)

        val dayStr = matcher.group(FLAG_DAY)

        val day = if (StringUtils.hasText(dayStr)) try {
            dayStr.toInt()
        } catch (e: NumberFormatException) {
            1
        } else {
            1
        }

        return InfoParam(user, mode.data!!, day, isMyself.get())
    }

    private fun InfoParam.getImage(): ByteArray {

        val BPs: List<Score> = try {
            scoreApiService.getBestPerformance(user.userID, mode, 0, 100)
        } catch (e: WebClientResponseException.NotFound) {
            throw InfoException(InfoException.Type.I_Player_NoBP, mode)
        } catch (e: Exception) {
            log.error("玩家信息：无法获取玩家 BP", e)
            throw InfoException(InfoException.Type.I_BP_FetchFailed)
        }


        val historyUser =
            infoDao.getLastFrom(
                user.userID,
                if (mode == OsuMode.DEFAULT) user.currentOsuMode else mode,
                LocalDate.now().minusDays(day.toLong())
            ).map { archive: OsuUserInfoArchiveLite? -> OsuUserInfoDao.fromArchive(archive) }

        return try {
            imageService.getPanelD(user, historyUser, BPs, mode)
        } catch (e: Exception) {
            log.error("玩家信息：图片渲染失败", e)
            throw InfoException(InfoException.Type.I_Fetch_Error)
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(InfoService::class.java)
    }
}
