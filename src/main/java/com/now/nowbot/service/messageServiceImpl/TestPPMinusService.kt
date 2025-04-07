package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.dao.PPMinusDao
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.json.LazerScore
import com.now.nowbot.model.json.OsuUser
import com.now.nowbot.model.ppminus.impl.PPMinus4
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.messageServiceImpl.PPMinusService.PPMinusParam
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.util.CmdUtil
import com.now.nowbot.util.CmdUtil.getMode
import com.now.nowbot.util.Instruction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.io.Serializable

@Service("TEST_PPM") class TestPPMinusService(
    private val scoreApiService: OsuScoreApiService,
    private val ppMinusDao: PPMinusDao,
    private val imageService: ImageService,
) : MessageService<PPMinusParam> {
    override fun isHandle(
        event: MessageEvent, messageText: String, data: MessageService.DataValue<PPMinusParam>
    ): Boolean {
        val matcher = Instruction.TEST_PPM.matcher(messageText)

        if (matcher.find().not()) return false

        val inputMode = getMode(matcher)
        val users = CmdUtil.get2User(event, matcher, inputMode, false)

        data.value = PPMinusParam(
            users.size == 2,
            users.first(),
            if (users.size == 2) users.last() else null,
            users.first().currentOsuMode
        )

        return true
    }

    override fun HandleMessage(event: MessageEvent, param: PPMinusParam) {
        val image = try {
            val my = getPPMinus4(param.me)
            val others = if (param.isVs) {
                getPPMinus4(param.other!!)
            } else null

            getPanelB1(param.me, param.other, my, others, param.mode)

        } catch (e: Exception) {
            log.error("PP-：渲染失败：", e)
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Render, "PPM")
        }

        try {
            event.reply(image)
        } catch (e: Exception) {
            log.error("PP-：发送失败：", e)
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Send, "PPM")
        }
    }

    private fun getPPMinus4(user: OsuUser): PPMinus4 {
        val bests: List<LazerScore>

        try {
            bests = scoreApiService.getBestScores(user)
        } catch (e: WebClientResponseException) {
            log.error("PP-：最好成绩获取失败", e)
            throw GeneralTipsException(GeneralTipsException.Type.G_Null_BP, user.username)
        }

        if (user.statistics.playTime < 60 || user.statistics.playCount < 30) {
            throw GeneralTipsException(
                GeneralTipsException.Type.G_NotEnough_PlayTime,
                user.currentOsuMode.fullName,
            )
        }

        try {
            ppMinusDao.savePPMinus(user, bests)
        } catch (e: Exception) {
            log.error("PP-：数据保存失败", e)
        }

        val surrounding = ppMinusDao.getSurroundingPPMinus(user, bests, 1000)

        try {
            return PPMinus4.getInstance(user, bests, surrounding, user.currentOsuMode)!!
        } catch (e: Exception) {
            log.error("PP-：数据计算失败", e)
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Calculate, "PPM")
        }
    }

    fun getPanelB1(me: OsuUser, other: OsuUser?, my: PPMinus4, others: PPMinus4?, mode: OsuMode): ByteArray {
        val cardA1s = ArrayList<OsuUser>(2)
        cardA1s.add(me)

        if (other != null) cardA1s.add(other)

        val titles = listOf("ACC", "PTT", "STA", "STB", "EFT", "STH", "OVA", "SAN")
        val cardB1 = my.values.mapIndexed { i, it -> titles[i] to it }.toMap()

        val cardB2 = others?.values?.mapIndexed { i, it -> titles[i] to it }?.toMap()

        val statistics: Map<String, Serializable> = mapOf("isVS" to (other != null), "gameMode" to mode.modeValue)

        val body = mutableMapOf(
            "card_A1" to cardA1s, "card_b_1" to cardB1, "statistics" to statistics
        )

        if (cardB2 != null) body["card_b_2"] = cardB2

        return imageService.getPanel(body.toMap(), "B1")
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(TestPPMinusService::class.java)
    }
}