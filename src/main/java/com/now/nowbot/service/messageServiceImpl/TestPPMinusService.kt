package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.dao.PPMinusDao
import com.now.nowbot.entity.PPMinusLite
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.model.ppminus.PPMinus4
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.messageServiceImpl.PPMinusService.PPMinusParam
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.util.InstructionUtil
import com.now.nowbot.util.Instruction
import org.slf4j.Logger
import org.slf4j.LoggerFactory

//@Service("TEST_PPM")
class TestPPMinusService(
    private val scoreApiService: OsuScoreApiService,
    private val ppMinusDao: PPMinusDao,
    private val imageService: ImageService,
) : MessageService<PPMinusParam> {
    override fun isHandle(
        event: MessageEvent, messageText: String, data: MessageService.DataValue<PPMinusParam>
    ): Boolean {
        val matcher = Instruction.TEST_PPM.matcher(messageText)

        if (matcher.find().not()) return false

        val mode = InstructionUtil.getMode(matcher)
        val users = InstructionUtil.get2User(event, matcher, mode, false)

        val me: OsuUser = users.first()
        val myBests: List<LazerScore> = scoreApiService.getBestScores(me.userID, mode.data!!, 0, 100)

        data.value = PPMinusParam(false, me, myBests, null, null, mode.data!!, -1)

        return true
    }

    override fun handleMessage(event: MessageEvent, param: PPMinusParam): ServiceCallStatistic? {
        val my = getPPMinus4(param.me)
        val others = if (param.isVs) {
            getPPMinus4(param.other!!)
        } else null

        val image = getPanelB1(param.me, param.other, my, others, param.mode)

        try {
            event.reply(image)
        } catch (e: Exception) {
            log.error("PP-：发送失败：", e)
            throw IllegalStateException.Send("PPM")
        }

        return null
    }

    private fun getPPMinus4(user: OsuUser): PPMinus4 {
        val bests: List<LazerScore> = scoreApiService.getBestScores(user)

        if (user.statistics!!.playTime!! < 60 || user.statistics!!.playCount!! < 30) {
            throw NoSuchElementException.PlayerPlayWithMode(user.username, user.currentOsuMode)
        }

        try {
            ppMinusDao.savePPMinus(user, bests)
        } catch (e: Exception) {
            log.error("PP-：数据保存失败", e)
        }

        var delta = 0
        val surrounding = run {
            var surrounding: List<PPMinusLite>

            do {
                delta += 500
                surrounding = ppMinusDao.getSurroundingPPMinus(user, bests, delta)
            } while (delta < 3000 && surrounding.size < 50)

            return@run surrounding
        }

        try {
            return PPMinus4.getInstance(user, bests, surrounding, delta, user.currentOsuMode)!!
        } catch (e: Exception) {
            log.error("PP-：数据计算失败", e)
            throw IllegalStateException.Calculate("PPM")
        }
    }

    fun getPanelB1(me: OsuUser, other: OsuUser?, my: PPMinus4, others: PPMinus4?, mode: OsuMode): ByteArray {
        val cardA1s = ArrayList<OsuUser>(2)
        cardA1s.add(me)

        if (other != null) cardA1s.add(other)

        val titles =
            listOf("ACC", "PTT", "STA", if (mode == OsuMode.MANIA) "PRE" else "STB", "EFT", "STH", "OVA", "SAN")
        val cardB1 = my.values.mapIndexed { i, it -> titles[i] to it }.toMap()

        val cardB2 = others?.values?.mapIndexed { i, it -> titles[i] to it }?.toMap()

        val statistics: Map<String, Any> = mapOf("is_vs" to (other != null), "mode_int" to mode.modeValue)

        val body = mutableMapOf(
            "users" to cardA1s, "my" to cardB1, "stat" to statistics, "count" to my.count, "delta" to my.delta, "panel" to "PM4"
        )

        if (cardB2 != null) body["others"] = cardB2

        return imageService.getPanel(body.toMap(), "B1")
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(TestPPMinusService::class.java)
    }
}