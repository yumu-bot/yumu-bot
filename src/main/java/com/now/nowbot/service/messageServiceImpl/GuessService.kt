package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.config.Permission
import com.now.nowbot.dao.BindDao
import com.now.nowbot.dao.ServiceCallStatisticsDao
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.osu.Covers
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.message.MessageChain.MessageChainBuilder
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.messageServiceImpl.GuessService.GuessGame.Hint.*
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.TipsException
import com.now.nowbot.util.*
import com.now.nowbot.util.command.FLAG_ID
import com.now.nowbot.util.command.FLAG_NAME
import com.now.nowbot.util.command.REG_SPACE
import kotlinx.coroutines.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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
    private val beatmapApiService: OsuBeatmapApiService,
    private val scoreApiService: OsuScoreApiService,
    private val imageService: ImageService,
    private val serviceCallStatisticsDao: ServiceCallStatisticsDao,
    private val calculateApiService: OsuCalculateApiService,
    private val bindDao: BindDao
): MessageService<GuessService.GuessParam> {

    companion object {
        private val guessScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        private val CURRENT_GAMES = ConcurrentHashMap<Long, GuessGame>()

        private val log: Logger = LoggerFactory.getLogger(javaClass)

        const val SIMILARITY_THRESHOLD = 0.7
        const val GUESS_THRESHOLD = 0.5

        suspend fun stopAllGamesFromReboot() {
            coroutineScope {
                CURRENT_GAMES.values
                    .sortedByDescending { it.lastPlayedTime }
                    .take(10)
                    .forEach { g ->
                        launch {
                            try {
                                delay(200L.milliseconds)

                                g.event.reply("猜歌被迫结束：服务器重启。")

                                log.info("在群组 ${g.event.subject.contactID} 的猜词游戏重启通知发送成功")
                            } catch (e: Exception) {
                                log.warn("发送重启通知失败: ${e.message}")
                            }
                        }
                    }
            }
        }
    }

    sealed class GuessParam {
        data class GuessStartParam(
            val user: OsuUser,
            val scores: List<LazerScore>,
            val size: Int = 10
        ): GuessParam()

        data object GuessEndParam: GuessParam()

        data class GuessOpenParam(val char: Char): GuessParam()

        data class GuessingParam(val result: String, val select: Int? = null): GuessParam()

        data class GuessTipParam(val select: Int? = null): GuessParam()
    }

    @Scheduled(fixedRate = 60000)
    fun cleanUpOvertimeGames() {
        if (CURRENT_GAMES.isEmpty()) return

        val iterator = CURRENT_GAMES.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val game = entry.value

            if (game.overtimeIn5Min && !game.send5Min.get()) {
                game.event.reply("""
                    猜歌还有 5 分钟自动结束。如果想参加可以输入 !g 字母或歌名。
                    您也可以输入 !g #序号 来获取对应题目的提示。
                    但注意：获取提示后，再猜出结果的得分会减少。
                """.trimIndent())
                game.send5Min.set(true)
            }

            if (game.overtime) {
                iterator.remove()

                game.event.replyDone(game, "由于长时间无人操作，猜歌已经自动结束。期待您的下次游玩！")
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

        val decrypted: MutableList<Int> = CopyOnWriteArrayList(MutableList(scores.size) { Hint.ALL_LOCKED })
        val rewards: MutableList<Int> = CopyOnWriteArrayList(MutableList(scores.size) { 100 })

        val startTime: LocalDateTime = LocalDateTime.now()
        var lastPlayedTime: LocalDateTime = startTime

        var send5Min: AtomicBoolean = AtomicBoolean(false)

        val overtimeIn5Min: Boolean
            get() = lastPlayedTime.plusMinutes(5).isBefore(LocalDateTime.now())

        val overtime: Boolean
            get() = lastPlayedTime.plusMinutes(10).isBefore(LocalDateTime.now())

        fun update() {
            this.lastPlayedTime = LocalDateTime.now()
            this.send5Min.set(false)
        }

        enum class Hint {
            RANKED_OR_UPDATED_TIME,
            SOURCE_OR_TAG,
            LANGUAGE,
            GENRE,
            DIFFICULTY_NAME,
            ARTIST,
            CREATOR,
            AUDIO,
            COVER,

            ;
            // 计算当前枚举项对应的位
            val mask: Int get() = 1 shl this.ordinal

            companion object {
                // 计算所有位都开启时的最大值 (511)
                val ALL_LOCKED: Int = entries.fold(0) { acc, hint -> acc or hint.mask }

                fun isLocked(mask: Int, hint: Hint): Boolean = (mask and hint.mask) != 0

                fun unlock(mask: Int, hint: Hint): Int = mask and hint.mask.inv()

                fun getRemaining(mask: Int): List<Hint> = entries.filter { isLocked(mask, it) }
            }
        }

        fun reward(index: Int): Int {
            val currentMask = decrypted[index]

            if (currentMask < 0) return 0

            // 1. 计算提示次数带来的权重衰减 (50, 25, 12...)
            val guessedHints = Hint.entries.size - Hint.getRemaining(currentMask).size
            val baseScore = 100 shr guessedHints

            // 2. 计算字符展示比例带来的修正
            // 逻辑：如果展示了 40% 的字符，则分数只剩原有的 60%
            val score = scores[index]

            val uniqueChars = if (unicode) {
                score.beatmapset.titleUnicode
            } else {
                score.beatmapset.title
            }.toCharArray()
                .map { DataUtil.getStandardisedString(it.toString()) }
                .toSet()

            val totalCharsCount = uniqueChars.size

            val revealedCount = uniqueChars.count { it in standardisedLetters }

            val visibilityFactor = if (totalCharsCount > 0) {
                (totalCharsCount - revealedCount).toDouble() / totalCharsCount
            } else {
                1.0
            }

            // 3. 最终得分 = 基准分 * 隐藏比例
            return (baseScore * visibilityFactor).toInt().coerceAtLeast(0)
        }

        fun tip(index: Int, beatmapApiService: OsuBeatmapApiService): MessageChain {
            val currentMask = decrypted[index]

            if (currentMask < 0) {
                throw TipsException("这张谱面已经被猜出来了，杂鱼！")
            }

            val remaining = Hint.getRemaining(currentMask)

            if (remaining.size <= 2) {
                throw TipsException("已经给了你足够多的提示了，杂鱼！")
            }

            val hint = remaining.random()
            decrypted[index] = Hint.unlock(currentMask, hint)

            return buildHintMessage(index, hint, beatmapApiService)
        }

        private fun buildHintMessage(index: Int, hint: Hint, beatmapApiService: OsuBeatmapApiService): MessageChain {
            val s = scores[index].beatmapset
            val b = scores[index].beatmap

            val i = index + 1

            return when (hint) {
                RANKED_OR_UPDATED_TIME -> if (s.rankedDate != null) {
                    MessageChain("谱面 #$i 的上架时间为：${s.rankedDate!!.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))}")
                } else {
                    MessageChain("谱面 #$i 的上次更新时间为：${s.lastUpdated.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))}")
                }

                SOURCE_OR_TAG -> if (s.source.isEmpty()) {
                    MessageChain("谱面 #$i 没有来源。它的部分标签是：${s.tags
                        .split(REG_SPACE.toRegex()).shuffled().take(5).joinToString(", ")
                    }。")
                } else {
                    MessageChain("谱面 #$i 的来源是 ${s.source}。")
                }
                LANGUAGE -> MessageChain("谱面 #$i 的语言是 ${DataUtil.getLanguageName(s.languageID)}。")
                DIFFICULTY_NAME -> MessageChain("谱面 #$i 的难度名是 ${b.difficultyName}。")
                GENRE -> MessageChain("谱面 #$i 的风格是 ${DataUtil.getGenreName(s.genreID)}。")
                ARTIST -> MessageChain("谱面 #$i 的艺术家是 ${s.artistUnicode}。")
                CREATOR -> if (b.owners.isNullOrEmpty()) {
                    MessageChain("谱面 #$i 的创建者是 ${s.creator}。")
                } else {
                    MessageChain("谱面 #$i 的谱师是 ${b.owners!!.joinToString(", ") { it.username }}。")
                }
                AUDIO -> if (s.availability.downloadDisabled){
                    MessageChain("谱面 #$i 貌似有版权限制。")
                } else {
                    val data = beatmapApiService.getVoice(s.beatmapsetID)

                    if (data != null) {
                        MessageChainBuilder().addVoice(data).build()
                    } else {
                        MessageChain("""
                            下载谱面 #$i 的音频失败了。给你个其他的提示：
                            它的难度是 ${"%.2f".format(b.starRating)}。
                        """.trimIndent())
                    }
                }
                COVER -> {
                    val image = beatmapApiService.getCover(s.covers, Covers.Companion.CoverType.COVER)

                    if (image != null) {
                        MessageChain("谱面 #$i 的背景图片是：", image)
                    } else {
                        MessageChain("""
                            下载谱面 #$i 的背景图片失败了。给你个其他的提示：
                            它${if (s.video) "有" else "没有"}视频，并且${if (s.storyboard) "有" else "没有"}故事板。
                        """.trimIndent())
                    }
                }
            }
        }

        /**
         * 当自动触发时，会返回 null，并且执行 onAutoDecrypted
         */
        fun reveal(char: Char, onAutoDecrypted: (List<Int>) -> Unit = {}): Boolean? {
            update()

            val c = char.lowercaseChar()

            if (c.isWhitespace() || standardisedLetters.contains(c.toString())) {
                return false
            }

            revealedLetters.add(c)
            standardisedLetters.add(DataUtil.getStandardisedString(c.toString()))

            val autoDecryptedIndices = mutableListOf<Int>()

            scores.forEachIndexed { i, score ->
                if (decrypted[i] >= 0) {
                    val s = score.beatmapset

                    val titleRevealed = isFullyRevealed(s.title)
                    val unicodeRevealed = unicode && isFullyRevealed(s.titleUnicode)

                    if (titleRevealed || unicodeRevealed) {
                        rewards[i] = reward(i)
                        decrypted[i] = -1
                        autoDecryptedIndices.add(i)
                    }
                }
            }

            if (autoDecryptedIndices.isNotEmpty()) {
                onAutoDecrypted(autoDecryptedIndices)
                return null
            }

            return true
        }

        private fun isFullyRevealed(text: String): Boolean {
            val validText = text.replace(Regex("[^a-zA-Z0-9]"), "")

            if (validText.isEmpty()) return true

            val revealedCount = validText.count { c ->
                val std = DataUtil.getStandardisedString(c.toString())
                std in standardisedLetters
            }

            return revealedCount >= validText.length * 0.9
        }
        fun encrypt(text: String): String {
            val charArray = text.toCharArray()

            for (i in charArray.indices) {
                val c = charArray[i]
                val std = DataUtil.getStandardisedString(c.toString())

                if (!c.isWhitespace() && std !in standardisedLetters) {
                    charArray[i] = '#'
                }
            }

            return String(charArray)
        }

        fun guess(text: String): List<Int> {
            update()

            val result = scores.mapIndexed { i, b ->
                if (decrypted[i] < 0) {
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
            result.forEach { i ->
                rewards[i] = reward(i)
                this.decrypted[i] = -1
            }

            return result
        }

        private val isSettled = AtomicBoolean(false)

        val done: Boolean
            get() = this.decrypted.all { it < 0 }

        fun trySettle(): Boolean {
            return done && isSettled.compareAndSet(false, true)
        }

        fun decryptAll() {
            update()

            decrypted.fill(-1)
        }

        fun getTextMessage(): MessageChain {
            val builder = StringBuilder()

            for (i in scores.indices) {
                val score = scores[i]

                if (decrypted[i] < 0) {
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

                    if (decrypted[i] >= 0) {
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
                        if (param.select != null) {
                            throw TipsException("#${param.select} 不是这个答案呢。")
                        } else {
                            throw TipsException("没有猜中歌曲呢。")
                        }
                    } else {
                        throw TipsException("开了这么多词都猜不中，真是杂鱼。")
                    }
                } else {
                    handleDecrypted(game, event, "您已经猜中以下谱面：", result)
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

                beatmapApiService.applyBeatmapExtend(selected)
                calculateApiService.applyStarToScores(selected)

                val game = GuessGame(param.user, selected, event = event, artist = false, unicode = false)
                CURRENT_GAMES[groupID] = game

                event.replyGuess(game, "猜歌开始，当前将选用 ${param.user.username} 的 ${param.user.currentOsuMode.fullName} 模式下，最好成绩中的 ${selected.size} 张谱面。")
            }

            is GuessParam.GuessEndParam -> {
                if (game == null) {
                    throw TipsException("无法结束不存在的猜歌。")
                }

                if (game.event.sender.contactID != event.sender.contactID && Permission.isGroupAdmin(event)) {
                    throw TipsException("""
                        你倒是猜啊？
                        只有群主、超级管理员或猜歌发起者可以通过 !g 停止当前猜歌。
                        您也可以等待 10 分钟，猜歌会因为超时而结束。
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

                val revealed = game.reveal(param.char, { result ->
                    handleDecrypted(game, event, "以下谱面已自动解开：", result)
                })

                if (revealed == false) {
                    throw TipsException("""
                        已经猜过 ${param.char} 了。
                        猜过的字母：${game.revealedLetters.joinToString(", ")}
                    """.trimIndent())
                } else if (revealed != null) {
                    event.replyGuess(game, "当前已开字母：${game.revealedLetters.joinToString(", ")}")
                }
            }

            is GuessParam.GuessTipParam -> {
                if (game == null) throw TipsException("猜歌异常：没有找到这个猜歌信息。")
                if (param.select == null) throw TipsException("想要全部的提示？门都没有！")

                if (param.select !in 0..game.scores.size) throw TipsException("你选中了棍木。")

                val message = game.tip(param.select, beatmapApiService)

                event.reply(message)
            }
        }

        return null
    }

    private fun handleDecrypted(
        game: GuessGame,
        event: MessageEvent,
        tip: String = "以下谱面已自动解开：",
        result: List<Int>
    ) {
        val reply = StringBuilder("$tip\n\n")

        result.forEachIndexed { i, r ->
            val b = game.scores[r]
            val set = b.beatmapset

            val title = if (game.unicode) set.titleUnicode else set.title

            // 根据 unicode 开关选择显示名称
            val decrypt = if (game.unicode) {
                "${set.artistUnicode} - ${set.titleUnicode}"
            } else {
                "${set.artist} - ${set.title}"
            }

            reply.append("#${r + 1}: $decrypt")

            if (i < result.size - 1) reply.append('\n')

            val isLuckyGuess = (game.standardisedLetters.isEmpty() || (
                    title.replace(Regex("[^a-zA-Z0-9]"), "").all { c ->
                        val std = DataUtil.getStandardisedString(c.toString())

                        std !in game.standardisedLetters
                    })) && game.decrypted[r] == GuessGame.Hint.ALL_LOCKED

            if (isLuckyGuess) {
                game.rewards[r] = 120
                reply.append("\n一击即中！\n得分：120")
            } else {
                reply.append("\n得分：${game.rewards[r]}")
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

    fun getParam(event: MessageEvent, matcher: Matcher, isGuessing: Boolean = false): GuessParam {
        if (!isGuessing) {
            return getStartParam(event, matcher)
        }

        return getOtherParam(matcher)
    }

    fun getStartParam(event: MessageEvent, matcher: Matcher): GuessParam {
        val mode = InstructionUtil.getMode(matcher, bindDao.getGroupModeConfig(event))

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
        val select = (matcher.group(FLAG_ID) ?: "").trim().toIntOrNull()

        return if (select != null && anything.isEmpty()) {
            GuessParam.GuessTipParam(select - 1)
        } else if (anything.isEmpty()) {
            GuessParam.GuessEndParam
        } else if (anything.length == 1) {
            GuessParam.GuessOpenParam(anything[0])
        } else {
            GuessParam.GuessingParam(anything, select?.let { it - 1 })
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
            .filter { it.second < 0 }
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
                ServiceCallStatistic.builds(game.event, beatmapsetIDs = decryptedSetIDs).apply {
                    this.setOther("GUESS", game.startTime.atZone(ZoneId.systemDefault()).toEpochSecond() * 1000, 0L)
                }
            )
        } else null
    }
}