package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.maimai.MaiBestScore
import com.now.nowbot.model.maimai.MaiFit.ChartData
import com.now.nowbot.model.maimai.MaiScore
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.divingFishApiService.MaimaiApiService
import com.now.nowbot.service.messageServiceImpl.MaiDistributionService.MaiDistScore
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.util.AsyncMethodExecutor
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.MaimaiUtil
import com.now.nowbot.util.command.FLAG_2_USER
import com.now.nowbot.util.command.FLAG_QQ_ID
import com.now.nowbot.util.command.REG_SEPERATOR_NO_SPACE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Service
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Matcher

@Service("MAI_VERSUS")
class MaiVersusService(
    private val maimaiApiService: MaimaiApiService,
    private val imageService: ImageService,
): MessageService<MaiVersusService.MaiVersusParam> {
    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<MaiVersusParam>
    ): Boolean {
        val matcher = Instruction.MAI_VERSUS.matcher(messageText)

        if (!matcher.find()) {
            return false
        }

        data.value = getParam(event, matcher)
        return true
    }

    override fun handleMessage(
        event: MessageEvent,
        param: MaiVersusParam
    ): ServiceCallStatistic? {
        val image = imageService.getPanel(param.toVersusResult(), "Theta")

        event.reply(image)

        return ServiceCallStatistic.building(event)
    }

    data class MaiVersusParam(
        val my: MaiBestScore,
        val others: MaiBestScore,
    )

    enum class MaiVersusStatus {
        // 完全打不过，完了...
        DOOMED,

        // 勉强打不过，不好...
        DAMN,

        // 勉强打得过，那就来吧...
        DRAW,

        // 完全打得过，找死...
        DOMINATE,

        ;
    }

    private fun getParam(event: MessageEvent, matcher: Matcher): MaiVersusParam {
        val my: MaiBestScore
        val others: MaiBestScore

        val nameStr = (matcher.group(FLAG_2_USER) ?: "").trim()
        val qqStr = (matcher.group(FLAG_QQ_ID) ?: "").trim()

        val targetQQ = if (event.hasAt()) {
            event.target
        } else if (qqStr.isNotBlank()) {
            qqStr.toLongOrNull()
        } else {
            null
        }

        if (targetQQ != null) {
            runBlocking(Dispatchers.IO) {
                val meDeferred = async {
                    maimaiApiService.getMaimaiBest50(event.sender.contactID)
                }

                val otherDeferred = async {
                    maimaiApiService.getMaimaiBest50(targetQQ)
                }

                my = meDeferred.await()
                others = otherDeferred.await()
            }
        } else if (nameStr.isNotBlank()) {
            val names = nameStr.split(REG_SEPERATOR_NO_SPACE.toRegex())

            when(names.size) {
                2 -> runBlocking(Dispatchers.IO) {
                    val meDeferred = async {
                        maimaiApiService.getMaimaiBest50(names[0].trim())
                    }

                    val otherDeferred = async {
                        maimaiApiService.getMaimaiBest50(names[1].trim())
                    }

                    my = meDeferred.await()
                    others = otherDeferred.await()
                }

                else -> runBlocking(Dispatchers.IO) {
                    val meDeferred = async {
                        maimaiApiService.getMaimaiBest50(event.sender.contactID)
                    }

                    val otherDeferred = async {
                        maimaiApiService.getMaimaiBest50(nameStr.trim())
                    }

                    my = meDeferred.await()
                    others = otherDeferred.await()
                }
            }
        } else {
            throw NoSuchElementException.Compare()
        }

        return MaiVersusParam(my, others)
    }

    private fun MaiVersusParam.toVersusResult(): Map<String, Any> {
        // 多线程获取歌曲的的 ChartData
        fun getChartData(scores: List<MaiScore>): Map<Long, ChartData> {
            ConcurrentHashMap<Long, ChartData>()

            val actions = scores.map {
                Callable {
                    it.independentID to
                            (maimaiApiService.getMaimaiChartData(it.songID).getOrNull(it.index) ?: ChartData())
                }
            }

            return AsyncMethodExecutor.awaitList(actions).toMap()
        }


        // 计算对应成绩的对应 DX 评分
        fun getDistsRating(score: MaiScore, chart: ChartData): Int {
            val star = if (chart.fit > 0.0) {
                chart.fit
            } else {
                score.star
            }

            val achievements = score.achievements

            return MaimaiUtil.getRating(star, achievements)
        }


        // 打包
        fun getMaiFitChartData(scores: List<MaiScore>): List<MaiDistScore> {
            val chartData = getChartData(scores)

            return scores.map {
                val chart = chartData[it.independentID] ?: ChartData()
                val rating = getDistsRating(it, chart)

                return@map MaiDistScore(it, chart, rating)
            }
        }

        val ms = buildList {
            addAll(my.charts.standard)
            addAll(my.charts.deluxe)
        }

        val os = buildList {
            addAll(others.charts.standard)
            addAll(others.charts.deluxe)
        }

        runBlocking(Dispatchers.IO) {
            val task1 = async { maimaiApiService.insert(my.charts) }
            val task2 = async { maimaiApiService.insert(others.charts) }

            task1.await()
            task2.await()
        }

        val mr = getMaiFitChartData(ms)
            .sortedByDescending { it.rating }

        val or = getMaiFitChartData(os)
            .sortedByDescending { it.rating }

        val mf = mr.sumOf { it.rating }
        val of = or.sumOf { it.rating }

        val delta = mf - of

        val status = when {
            delta >= 300 -> MaiVersusStatus.DOMINATE
            delta >= 0 -> MaiVersusStatus.DRAW
            delta > -300 -> MaiVersusStatus.DAMN
            else -> MaiVersusStatus.DOOMED
        }

        val (winnerData, loserData) = if (delta >= 0) {
            mr to or
        } else {
            or to mr
        }

        // 实力
        val winnerIDs = winnerData
            .sortedByDescending { it.rating - it.score.rating }
            .take(5)
            .map { it.score.songID % 10000 }

        val winnerSet = winnerIDs.toSet()

        val loserIDs = if (status == MaiVersusStatus.DOOMED || status == MaiVersusStatus.DOMINATE) {
            // 水图
            loserData
                .filter { it.score.songID % 10000 !in winnerSet }
                .sortedBy { it.rating - it.score.rating }
                .take(5)
                .map { it.score.songID % 10000 }
        } else {
            // 实力
            loserData
                .filter { it.score.songID % 10000 !in winnerSet }
                .sortedByDescending { it.rating - it.score.rating }
                .take(5)
                .map { it.score.songID % 10000 }
        }

        val (m5, o5) = if (delta >= 0) {
            winnerIDs to loserIDs
        } else {
            loserIDs to winnerIDs
        }

        return mapOf(
            "me" to my.getUser(),
            "other" to others.getUser(),
            "my_dist" to mf,
            "other_dist" to of,
            "my" to m5,
            "others" to o5,
            "status" to status.name,
        )
    }
}