package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.dao.MaiDao
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.maimai.MaiBestScore
import com.now.nowbot.model.maimai.MaiScore
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.divingFishApiService.MaimaiApiService
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.*
import org.springframework.stereotype.Service
import java.util.regex.Matcher
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

@Service("MAI_FILTER")
class MaiFilterService(
    private val maimaiApiService: MaimaiApiService,
    private val imageService: ImageService,
    private val maiDao: MaiDao
): MessageService<MaiFilterService.MaiFilterParam> {

    data class MaiFilterParam(
        val user: MaiBestScore.User,
        val score: List<MaiScore>,
        val page: Int,
        val type: String
    ) {
        fun toMap(): Map<String, Any> {
            val maxPage = ceil(score.size / 50.0).toInt()

            val page = max(min(this.page, maxPage), 1)

            return mapOf(
                "user" to user,
                "score" to score.drop((page - 1) * 50).take(50),
                "page" to page,
                "max_page" to maxPage,
                "type" to type,
            )
        }
    }

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<MaiFilterParam>
    ): Boolean {
        val matcher: Matcher

        val ap = Instruction.MAI_AP.matcher(messageText)
        val fc = Instruction.MAI_FC.matcher(messageText)
        val type: String

        if (fc.find()) {
            matcher = fc
            type = "fc"
        } else if (ap.find()) {
            matcher = ap
            type = "ap"
        } else {
            return false
        }

        val nameStr: String? = matcher.group(FLAG_NAME)
        val pageStr: String? = matcher.group(FLAG_PAGE)
        val qqStr: String? = matcher.group(FLAG_QQ_ID)

        val user: MaiBestScore.User
        val page: Int

        if (!qqStr.isNullOrBlank()) {
            user = maimaiApiService.getMaimaiBest50(qqStr.toLongOrNull() ?: event.sender.id).getUser(maiDao)
            page = pageStr?.toIntOrNull() ?: 1
        } else if (!nameStr.isNullOrBlank()) {
            if (nameStr.matches("\\w+\\s+$REG_NUMBER_1_100".toRegex())) {
                val names = nameStr.split("\\s+".toRegex())

                user = maimaiApiService.getMaimaiBest50(names.first()).getUser(maiDao)
                page = names.last().toIntOrNull() ?: 1

            } else if (nameStr.matches(REG_NUMBER_1_100.toRegex())) {

                user = maimaiApiService.getMaimaiBest50(event.sender.id).getUser(maiDao)
                page = nameStr.trim().toIntOrNull() ?: 1

            } else {
                user = maimaiApiService.getMaimaiBest50(nameStr.trim()).getUser(maiDao)
                page = pageStr?.toIntOrNull() ?: 1
            }
        } else {
            user = maimaiApiService.getMaimaiBest50(event.sender.id).getUser(maiDao)
            page = pageStr?.toIntOrNull() ?: 1
        }

        val scores = maimaiApiService.getMaimaiFullScores(user.proberName!!).records.filter {
            if (type == "ap") {
                it.combo == "ap"
            } else {
                it.combo == "fc"
            }
        }.sortedByDescending { it.rating }

        maimaiApiService.insert(scores)

        if (scores.isEmpty()) throw NoSuchElementException.BestScoreFiltered(user.name!!)

        data.value = MaiFilterParam(user, scores, page, type)
        return true
    }

    override fun handleMessage(event: MessageEvent, param: MaiFilterParam): ServiceCallStatistic? {
        val image = imageService.getPanel(param.toMap(), "MI")

        event.reply(image)

        return ServiceCallStatistic.building(event) {
            setParam(mapOf(
                "mais" to param.score.map { it.songID }
            ))
        }
    }
}