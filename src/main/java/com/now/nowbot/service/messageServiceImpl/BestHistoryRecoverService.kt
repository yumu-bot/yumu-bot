package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.config.Permission
import com.now.nowbot.dao.BindDao
import com.now.nowbot.dao.ScoreDao
import com.now.nowbot.dao.UserSnapShotDao
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.throwable.botRuntimeException.PermissionException
import com.now.nowbot.util.ASyncMessageUtil
import com.now.nowbot.util.AsyncMethodExecutor
import com.now.nowbot.util.DataUtil
import com.now.nowbot.util.FastPower095
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.InstructionUtil
import com.now.nowbot.util.UserIDUtil
import com.now.nowbot.util.command.REG_HASH
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter
import java.util.regex.Matcher

@Service("BP_HISTORY")
class BestHistoryRecoverService(
    private val userApiService: OsuUserApiService,
    private val beatmapApiService: OsuBeatmapApiService,
    private val calculateApiService: OsuCalculateApiService,
    private val imageService: ImageService,
    private val bestSnapShotDao: UserSnapShotDao,
    private val scoreDao: ScoreDao,
    private val bindDao: BindDao
): MessageService<BestHistoryRecoverService.BestHistoryParam> {
    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<BestHistoryParam>
    ): Boolean {
        val matcher = Instruction.BP_HISTORY.matcher(messageText)

        if (!matcher.find()) {
            return false
        }

        if (!Permission.isSuperAdmin(event)) {
            throw PermissionException.DeniedException.BelowSuperAdministrator()
        }

        data.value = getParam(event, matcher)

        return true
    }

    override fun handleMessage(
        event: MessageEvent,
        param: BestHistoryParam
    ): ServiceCallStatistic? {
        val selected = param.replyAndGetSelected(event) ?: return null

        val chain = param.getMessageChain(selected - 1)

        event.reply(chain)

        return ServiceCallStatistic.building(event)
    }

    private fun getParam(event: MessageEvent, matcher: Matcher): BestHistoryParam {
        val mode = InstructionUtil.getMode(matcher, bindDao.getGroupModeConfig(event))
        val id = UserIDUtil.getUserIDWithoutRange(event, matcher, mode)

        val user: OsuUser

        if (id != null) {
            user = runCatching {
                userApiService.getOsuUser(id, mode.data!!)
            }.getOrElse {
                OsuUser(id = id).apply { this.mode = mode.data!!.shortName }
            }

        } else {
            user = InstructionUtil.getUserWithoutRange(event, matcher, mode)
        }

        return BestHistoryParam(user, OsuMode.getMode(user.currentOsuMode, mode.data!!))
    }

    private fun BestHistoryParam.replyAndGetSelected(event: MessageEvent): Int? {
        val countPerPage = 5

        val count = bestSnapShotDao.getCount(this.user.userID, this.mode)

        if (count == 0L) {
            throw NoSuchElementException.Snapshot()
        }

        var current = event
        var continuing = true
        var page = 1
        val maxPage = ((count / 5) + 1).toInt().coerceAtLeast(1)

        while (continuing) {
            // 2. 获取数据 (例如展示当前页)
            val created = bestSnapShotDao.getCreatedAt(this.user.userID, this.mode,
                page.coerceIn(1, maxPage) - 1, countPerPage)

            // 发送预览内容 (根据需要更新你的回复内容)
            val sb = StringBuilder("当前玩家可追溯的预览切片：")

            sb.append("\n\n")

            sb.append(created.mapIndexed { i, c ->
                "#${i + 1}: ${c.format(formatter)}"
            }.joinToString("\n"))

            sb.append("\n\n")

            sb.append("您可以输入对应的编号，或是含有 # 号的页码来翻页。\n")
            sb.append("当前第 $page 页，总共第 $maxPage 页")

            current.reply(sb.toString())

            // 3. 等待后续交互
            val lock = ASyncMessageUtil.getLock(current, 30 * 1000)
            val next = lock.get()

            if (next != null) {
                current = next

                val index = next.rawMessage.replace(REG_HASH.toRegex(), "").toIntOrNull()
                val hash = next.rawMessage.contains(REG_HASH.toRegex())

                if (index != null) {
                    if (hash && index in 1..maxPage) {
                        page = index.coerceAtLeast(1)
                    } else if (index in 1..countPerPage) {
                        return index + (page - 1) * countPerPage
                    } else if (index in countPerPage..count) {
                        return index
                    } else {
                        current.reply("请输入范围内的编号或页码！请重试。")
                        continuing = false
                    }
                } else {
                    current.reply("请输入正确的编号或页码！请重试。")
                    continuing = false
                }
            } else {
                // 超时，结束循环
                current.reply("已取消操作。").recallIn(30 * 1000)
                continuing = false
            }
        }

        return null
    }

    private fun BestHistoryParam.getMessageChain(index: Int): MessageChain {
        val snapshot = bestSnapShotDao.getWithOffset(this.user.userID, this.mode, index.coerceAtLeast(0))
            ?: throw NoSuchElementException.SnapshotSelected()

        val ids = snapshot.scoreIDs.toList()

        val map = scoreDao.getScoresFromIDs(ids).associateBy { it.scoreID }

        val scoreMap = List(ids.size) { i -> i + 1 }
            .zip(ids)
            .mapNotNull { (index, id) ->
                map[id]?.let { score -> index to score}
            }.toMap()

        val snapshotPP = scoreMap.map { (index, score) ->
            score.pp * FastPower095.pow(index - 1)
        }.sum() + DataUtil.getBonusPP(user.beatmapPlaycount)

        return if (scoreMap.size > 1) {
            val ranks = scoreMap.keys
            val scores = scoreMap.values

            AsyncMethodExecutor.awaitPair(
                { beatmapApiService.applyBeatmapExtend(scoreMap) },
                { calculateApiService.applyStarToScores(scoreMap) }
            )

            val body = mapOf(
                "user" to user.apply {
                    this.ppEstimate = snapshotPP
                    this.statistics!!.pp = 0.0 },
                "scores" to scores,
                "rank" to ranks,
                "panel" to "BH",
                "compact" to (scores.size > 100),
                "created_at" to snapshot.createdAt,
            )

            MessageChain(imageService.getPanel(body, "A4"))
        } else {

            val pair = scoreMap.toList().first()

            val score: LazerScore = pair.second
            score.ranking = pair.first

            val e5Param = ScorePRService.getE5ParamForFilteredScore(
                user.apply {
                    this.ppEstimate = snapshotPP
                    this.statistics!!.pp = 0.0 },
                null, score, "BH", beatmapApiService, calculateApiService)

            MessageChain(imageService.getPanel(e5Param.toMap(), "E5"))
        }
    }

    data class BestHistoryParam(
        val user: OsuUser,
        val mode: OsuMode
    )


    companion object {
        private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    }

}