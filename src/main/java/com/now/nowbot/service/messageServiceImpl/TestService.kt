package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.cache.QQMessageCacheProvider
import com.now.nowbot.config.Permission
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.osuApiService.OsuMatchApiService
import com.now.nowbot.throwable.TipsException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.math.roundToInt

@Service("TEST")
class TestService(
    private val matchApiService: OsuMatchApiService
) : MessageService<String> {
    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<String>
    ): Boolean {
        if (messageText.contains("!yuumu") && Permission.isSuperAdmin(event.sender.contactID)) {
            data.value = messageText.replaceFirst("!yuumu", "")
            return true
        } else {
            return false
        }
    }

    override fun handleMessage(event: MessageEvent, param: String): ServiceCallStatistic? {
        val ids = param.replace("\\s*".toRegex(), "").split(",")
            .map { it.toLongOrNull() ?: throw TipsException("转换 $it 失败") }

        val matches = ids.flatMap { i ->
            val m = matchApiService.getMatch(i)

            m.events.mapNotNull { e -> e.round }.flatMap { r ->
                r.scores.map { s -> r.beatmap?.previewName to s }
            }
        }

        // 2. 计算每个用户的平均分，并统计7和2的出现次数
        // 只取 LazerScore
        matches
            .map { it.second }  // 只取 LazerScore
            .groupBy { it.userID }
            .mapValues { (_, scores) -> scores.map { it.score }.average() }
            .map { (userId, avg) ->
                Triple(
                    userId,
                    avg,
                    avg.roundToInt().toString().count { it == '7' || it == '2' }
                )
            }
            .maxByOrNull { it.third }
            ?.let { event.replyAsync("""
                玩家：${it.first}，均分：${it.second}, 次数：${it.third}
            """.trimIndent()) }

        // 成绩 Acc 和总分加起来含数字 6 最多的玩家奖励 2个月 osu! supporter（等值 60 CNY）
        // 额外奖励：如果有任何一个成绩按照上述规则条件之和 ≥ 7，奖励 66.66 CNY （奖励可叠加并且不限人数）

        /*
        matches.map { (preview, score) ->
            val count = score.score.toString().count { it == '6' } + "%.2f".format(score.accuracy * 100).count { it == '6' }

            Triple(count, preview, score)
        }
            .sortedByDescending { it.first }
            .take(10)
            .forEachIndexed { index, (count, p, score) -> 
                
                event.replyAsync(
                    """
                        第${index+1}：${score.userID}：${count} 个
                        成绩：${score.score}
                        准确率：${"%.2f".format(score.accuracy * 100)}
                        谱面：${p}
                    """.trimIndent()
                )
            }

         */


        return null
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(TestService::class.java)
    }
}

