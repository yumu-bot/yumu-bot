package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.config.Permission
import com.now.nowbot.dao.ServiceCallStatisticsDao
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.TipsException
import com.now.nowbot.util.ASyncMessageUtil
import com.now.nowbot.util.AsyncMethodExecutor
import com.now.nowbot.util.DataUtil
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.InstructionUtil
import com.now.nowbot.util.UserIDUtil
import com.now.nowbot.util.command.FLAG_NAME
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Matcher
import kotlin.math.E
import kotlin.math.ln
import kotlin.math.pow
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

@Service("GUESS")
class GuessService(
    private val userApiService: OsuUserApiService,
    private val scoreApiService: OsuScoreApiService,
    private val imageService: ImageService,
    private val serviceCallStatisticsDao: ServiceCallStatisticsDao,
    private val calculateApiService: OsuCalculateApiService,
): MessageService<GuessService.GuessParam> {

    companion object {
        private val guessScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        private val CURRENT_GAMES = ConcurrentHashMap<Long, GuessGame>()

        const val SIMILARITY_THRESHOLD = 0.7
        const val GUESS_THRESHOLD = 0.5

        fun stopAllGamesFromReboot() {
            CURRENT_GAMES.values
                .sortedByDescending { it.lastPlayedTime }
                .take(5)
                .forEach { g ->
                guessScope.launch {
                    delay(500.milliseconds)
                    g.event.reply("""
                        猜歌被迫结束：服务器重启。
                    """.trimIndent())
                }
            }
        }
    }

    sealed class GuessParam {
        data class GuessStartParam(
            val user: OsuUser,
            val scores: List<LazerScore>,
        ): GuessParam()

        data object GuessEndParam: GuessParam()

        data class GuessOpenParam(val char: Char): GuessParam()

        data class GuessingParam(val result: String): GuessParam()
    }

    @Scheduled(fixedRate = 60000)
    fun cleanUpOvertimeGames() {
        if (CURRENT_GAMES.isEmpty()) return

        val iterator = CURRENT_GAMES.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val game = entry.value

            if (game.remain5Min) {
                game.event.reply("猜歌还有 5 分钟自动结束。如果想参加可以输入 !g 字母或歌名。")
            }

            if (game.overtime) {
                iterator.remove()

                game.event.replyDone(game, "由于长时间无人操作，猜歌已经自动结束。")
            }
        }
    }


    data class GuessGame(
        val user: OsuUser,
        val scores: List<LazerScore>,

        val event: MessageEvent,

        val artist: Boolean,
        val unicode: Boolean
    ) {
        val revealedLetters: MutableSet<Char> = mutableSetOf()
        val standardisedLetters: MutableSet<String> = mutableSetOf()

        val decrypted: MutableList<Boolean> = CopyOnWriteArrayList(MutableList(scores.size) { false })

        var lastPlayedTime: LocalDateTime = LocalDateTime.now()

        val remain5Min: Boolean
            get() = lastPlayedTime.plusMinutes(5).isBefore(LocalDateTime.now()) &&
                    lastPlayedTime.plusMinutes(5).plusSeconds(10).isAfter(LocalDateTime.now())

        val overtime: Boolean
            get() = lastPlayedTime.plusMinutes(10).isBefore(LocalDateTime.now())

        fun update() {
            this.lastPlayedTime = LocalDateTime.now()
        }

        fun reveal(char: Char): Boolean? {
            update()

            val c = char.lowercaseChar()

            if (standardisedLetters.contains(c.toString())) {
                return false
            }

            if (c.isWhitespace()) {
                return null
            }

            revealedLetters.add(c)
            standardisedLetters.add(DataUtil.getStandardisedString(c.toString()))

            return true
        }

        fun encrypt(text: String): String {
            val charArray = text.toCharArray()

            for (i in charArray.indices) {
                val c = charArray[i]
                val std = DataUtil.getStandardisedString(c.toString())

                if (!c.isWhitespace() && std !in standardisedLetters) {
                    charArray[i] = '*'
                }
            }

            return String(charArray)
        }

        fun guess(text: String): List<Int> {
            update()

            val result = scores.mapIndexed { i, b ->
                if (decrypted[i]) {
                    return@mapIndexed i to null
                }

                val s = b.beatmapset

                val t = DataUtil.getStringSimilarity(s.title, text)
                val u = DataUtil.getStringSimilarity(s.titleUnicode, text)

                return@mapIndexed i to (t >= GUESS_THRESHOLD || u >= GUESS_THRESHOLD)
            }
                .filter { it.second == true }
                .map { it.first }

            // 记得解开
            result.forEach { i -> this.decrypted[i] = true }

            return result
        }

        private val isSettled = AtomicBoolean(false)

        val done: Boolean
            get() = this.decrypted.all { it }

        fun trySettle(): Boolean {
            return done && isSettled.compareAndSet(false, true)
        }

        fun decryptAll() {
            update()

            decrypted.fill(true)
        }

        fun getTextMessage(): MessageChain {
            val builder = StringBuilder()

            for (i in scores.indices) {
                val score = scores[i]

                if (decrypted[i]) {
                    val decrypt = if (unicode) {
                        score.beatmapset.artistUnicode + " - " + score.beatmapset.titleUnicode
                    } else {
                        score.beatmapset.artist + " - " + score.beatmapset.title
                    }

                    builder.append("${i + 1}: ").append(decrypt).append('\n')
                } else {
                    val artist = if (this.artist) {
                        if (unicode) {
                            score.beatmapset.artistUnicode
                        } else {
                            score.beatmapset.artist
                        }.let { encrypt(it) } + " - "
                    } else {
                        ""
                    }

                    builder.append("${i + 1}: ").append(artist)

                    val title = if (unicode) {
                        score.beatmapset.titleUnicode
                    } else {
                        score.beatmapset.title
                    }.let { encrypt(it) }

                    builder.append(title).append('\n')
                }
            }

            return MessageChain(builder.toString())
        }

        fun toMap(): Map<String, Any> {
            return mapOf(
                "user" to user,
                "scores" to scores.mapIndexed { i, score ->
                    val s = score.beatmapset

                    if (!decrypted[i]) {
                        mapOf(
                            "beatmapset" to mapOf(
                                "title" to encrypt(s.title),
                                "title_unicode" to encrypt(s.titleUnicode),
                                "artist" to encrypt(s.artist),
                                "artist_unicode" to encrypt(s.artistUnicode),
                                "covers" to s.covers
                            ),

                            "beatmap" to score.beatmap,

                        )
                    } else {
                        score
                    }
                },
                "letters" to revealedLetters,
                "decrypted" to decrypted,

                "artist" to artist,
                "unicode" to unicode,
            )
        }
    }

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<GuessParam>
    ): Boolean {
        val matcher = Instruction.GUESS.matcher(messageText)

        if (!matcher.find()) {
            return false
        }

        val isGuessing = CURRENT_GAMES.keys.contains(event.subject.contactID)

        data.value = getParam(event, matcher, isGuessing)
        return true
    }

    override fun handleMessage(
        event: MessageEvent,
        param: GuessParam
    ): ServiceCallStatistic? {
        val groupID = event.subject.contactID
        val game = CURRENT_GAMES[groupID]

        when(param) {
            is GuessParam.GuessingParam -> {
                if (game == null) {
                    throw TipsException("猜歌异常：没有找到这个猜歌信息。")
                }

                val result = game.guess(param.result)

                if (result.isEmpty()) {
                    if (game.revealedLetters.size <= 8) {
                        throw TipsException("没有猜中歌曲呢。")
                    } else {
                        throw TipsException("给了这么多提示都猜不中，真是杂鱼。")
                    }
                } else {
                    val reply = StringBuilder("您已经猜中以下谱面：").append('\n').append('\n')

                    result.forEachIndexed { i, r ->
                        val b = game.scores[r]

                        val decrypt = if (game.unicode) {
                            b.beatmapset.artistUnicode + " - " + b.beatmapset.titleUnicode
                        } else {
                            b.beatmapset.artist + " - " + b.beatmapset.title
                        }

                        reply.append("#${r + 1}: $decrypt")

                        if (i <= result.size - 1) {
                            reply.append('\n')
                        }
                    }

                    event.replyGuess(game, reply.toString())

                    guessScope.launch {
                        if (game.trySettle()) {
                            delay(500.milliseconds)
                            event.replyDone(game, "猜歌结束。", noGuess = true)
                        }
                    }
                }
            }

            is GuessParam.GuessStartParam -> {
                if (game != null) {
                    throw TipsException("当前有正在进行中的猜歌，请等待此猜歌流程结束。")
                }

                // 之前已经猜过的歌曲 set ID
                val history = serviceCallStatisticsDao.getLast10BeatmapsetIDs(
                    groupID, "GUESS", LocalDateTime.now().minusMonths(3)
                )
                val frequencyMap = history.groupingBy { it }.eachCount()

                val selected = param.scores
                    .filter { score ->
                        val set = score.beatmapset
                        maxOf(set.title.length, set.titleUnicode.length) > 3
                    }
                    // 1. 先按你的随机权重降序排列，保证频率低的/随机分高的排在前面
                    .sortedByDescending { score ->
                        val count = frequencyMap[score.beatmapset.beatmapsetID] ?: 0
                        val penalty = ln(E + count * 2)
                        Random.nextDouble().pow(penalty)
                    }
                    // 2. 执行相似度去重
                    .let { sortedList ->
                        val result = mutableListOf<LazerScore>()
                        for (bm in sortedList) {
                            if (result.size >= 10) break

                            val currentTitle = bm.beatmapset.title

                            // 检查是否与已加入结果集的标题太像
                            val isTooSimilar = result.any { existing ->
                                DataUtil.getStringSimilarity(currentTitle, existing.beatmapset.title) > SIMILARITY_THRESHOLD
                            }

                            if (!isTooSimilar) {
                                result.add(bm)
                            }
                        }
                        result
                    }
                    .shuffled()

                if (selected.size < 8) {
                    throw TipsException("能够作为猜歌题目的最好成绩数量不够呢...")
                }

                calculateApiService.applyStarToScores(selected)

                val game = GuessGame(param.user, selected, event = event, artist = false, unicode = false)
                CURRENT_GAMES[groupID] = game

                event.replyGuess(game, "猜歌开始，当前将选用 ${param.user.username} 的最好成绩中的 ${selected.size} 张谱面。")
            }

            is GuessParam.GuessEndParam -> {
                if (game == null) {
                    throw TipsException("无法结束不存在的猜歌。")
                }

                if (game.event.sender.contactID != event.sender.contactID && Permission.isGroupAdmin(event)) {
                    throw TipsException("""
                        你倒是猜啊？
                        只有群主、超级管理员或猜歌发起者可以通过 !g 停止当前猜歌。
                    """.trimIndent())
                }

                val receipt = event.reply("您确定要结束当前的猜歌吗？回复 OK 确认。")
                receipt.recallIn(30 * 1000)

                val lock = ASyncMessageUtil.getLock(event, 30 * 1000)

                val ev = lock.get()

                if (ev != null && ev.rawMessage.contains("OK", ignoreCase = true)) {
                    ev.replyDone(game)
                } else {
                    receipt.recall()
                }

                // 照常进行
            }

            is GuessParam.GuessOpenParam -> {
                if (game == null) throw TipsException("猜歌异常：没有找到这个猜歌信息。")

                val revealed = game.reveal(param.char)

                if (revealed == false) {
                    throw TipsException("""
                        已经猜过 ${param.char} 了。
                        猜过的字母：${game.revealedLetters.joinToString(", ")}
                    """.trimIndent())
                } else if (revealed == null) {
                    throw TipsException("好慈祥的老奶奶啊。")
                }

                event.replyGuess(game, "当前已开字母：${game.revealedLetters.joinToString(", ")}")
            }
        }

        return null
    }

    fun getParam(event: MessageEvent, matcher: Matcher, isGuessing: Boolean = false): GuessParam {
        if (!isGuessing) {
            return getStartParam(event, matcher)
        }

        return getOtherParam(matcher)
    }

    fun getStartParam(event: MessageEvent, matcher: Matcher): GuessParam {
        val mode = InstructionUtil.getMode(matcher)

        val userID = UserIDUtil.getUserIDWithoutRange(event, matcher, mode)

        if (userID != null) {
            val (user, scores) = AsyncMethodExecutor.awaitPair(
                { userApiService.getOsuUser(userID, mode.data!!) },
                { scoreApiService.getBestScores(userID, mode.data!!) },
            )

            return GuessParam.GuessStartParam(user, scores)
        } else {
            val user = InstructionUtil.getUserWithoutRange(event, matcher, mode)
            val scores = scoreApiService.getBestScores(user.userID, mode.data!!)

            return GuessParam.GuessStartParam(user, scores)
        }
    }

    fun getOtherParam(matcher: Matcher): GuessParam {
        val anything = (matcher.group(FLAG_NAME) ?: "").trim()

        return if (anything.isEmpty()) {
            GuessParam.GuessEndParam
        } else if (anything.length == 1) {
            GuessParam.GuessOpenParam(anything[0])
        } else {
            GuessParam.GuessingParam(anything)
        }
    }

    private fun MessageEvent.replyGuess(game: GuessGame, text: String? = null) {

        runCatching {

            val image = imageService.getPanel(game.toMap(), "A8")

            if (text?.isNotBlank() == true) {
                this.reply(image, text)
            } else {
                this.reply(image)
            }

        }.onFailure {
            if (text?.isNotBlank() == true) {
                this.reply(text)
            }

            guessScope.launch {
                delay(500.milliseconds)
                this@replyGuess.reply(game.getTextMessage())
            }
        }
    }

    private fun MessageEvent.replyDone(
        game: GuessGame, text: String? = "猜歌结束。", noGuess: Boolean = false
    ): ServiceCallStatistic? {
        val decryptedSetIDs = game.scores.zip(game.decrypted)
            .filter { it.second }
            .map { it.first.beatmapset.beatmapsetID }
            .toSet()

        if (!noGuess) {
            game.decryptAll()
            game.event.replyGuess(game, text)
        }

        CURRENT_GAMES.remove(this.subject.contactID)

        // 没猜出，或是根本没猜的就不放进去了
        return if (decryptedSetIDs.isNotEmpty()) {
            serviceCallStatisticsDao.saveService(
                ServiceCallStatistic.builds(game.event, beatmapsetIDs = decryptedSetIDs)
            )
        } else null
    }
}