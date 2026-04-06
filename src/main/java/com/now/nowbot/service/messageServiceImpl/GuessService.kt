package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.config.Permission
import com.now.nowbot.dao.BindDao
import com.now.nowbot.dao.ServiceCallStatisticsDao
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.Covers
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.message.MessageChain.MessageChainBuilder
import com.now.nowbot.qq.tencent.TencentMessageService
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
): MessageService<GuessService.GuessParam>, TencentMessageService<GuessService.GuessParam> {

    companion object {
        private val KALEIDXSCOPE = CoroutineScope(Dispatchers.IO + SupervisorJob())
        private val CURRENT_GAMES = ConcurrentHashMap<Long, GuessGame>()

        private val GUESS_SPECIAL_WORDS_REGEX = Regex(
            "(?i)[(\\[<~-【]*\\b(" +
                    // 1. 合作与客串 (Artist/Collaboration)
                    "vs\\.?|versus|feat\\.?|ft\\.?" + "|" +
                    // 2. 配音标记 (Voice Actors)
                    "c\\.?v\\.?|v\\.?o\\.?" + "|" +
                    // 3. 常见版本前缀 + 常见后缀的组合 (TV Size, Game Ver, etc.)
                    "(?:tv|game|movie|short|long|extended|cut|spee?d\\s+up|nightcore)\\s+(?:size|edit|cut|ver(?:sion|\\.?)|mix)" + "|" +
                    // 4. 复合标记 (Combined tags)
                    "(?:nightcore|spee?d\\s+up)\\s+(&|and)\\s+cut\\s+ver(?:sion|\\.?)" + "|" +
                    // 5. 长度或版本变体 (Short, Long, etc.)
                    "short|long|extended" + "|" +
                    // 6. 通用万能匹配 (#### Ver.)
                    "[a-z0-9]+\\s+ver(?:sion|\\.?)" +
                    ")\\b[)\\]>~-】]*"
        )

        private fun String.getCore(): String {
            return this.replace(GUESS_SPECIAL_WORDS_REGEX, "")
                .replace(Regex("[^\\p{L}\\p{N}]"), "")
        }

        private val log: Logger = LoggerFactory.getLogger(GuessService::class.java)

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

                                g.event.reply(GuessReply.Restart)

                                log.info("在群组 ${g.event.subject.contactID} 的猜词游戏重启通知发送成功")
                            } catch (e: Exception) {
                                log.warn("发送重启通知失败: ${e.message}")
                            }
                        }
                    }
            }
        }
    }

    // 超级稳定™ 的抗腾讯检测回复类
    open class GuessReply(private val mixin: List<List<String>>) {
        override fun toString(): String {
            return mixin.joinToString("") { mix ->
                if (mix.isNotEmpty()) {
                    mix.random()
                } else {
                    ""
                }
            }.ifEmpty { "总之这是一条回复。" }
        }

        data object Restart: GuessReply(
            listOf(
                listOf("猜歌游戏", "本局猜歌", "正在进行的猜歌"),
                listOf("遇到服务器重启", "因为系统维护", "由于老大要睡觉"),
                listOf("已被迫中断。", "暂时无法继续。", "已经终止。", "已经结束了（我是来退出这个乐队的）。"),
                listOf("请稍后再试。", "很快就会恢复！", "抱歉！"),
            )
        )

        data object NearingTimeout: GuessReply(
            listOf(
                listOf(
                    "猜歌即将自动结束。",
                    "很长时间没有人继续猜歌了。"
                ),
                listOf(
                    "如果还想参加，可以输入 !g 字母或歌名。",
                    "人家还想一起玩呢。"
                ),
                listOf("\n"),
                listOf(
                    "如果不知道怎么猜，可以输入 !g #序号 来获取提示。但要注意这样可能会损失得分。",
                    "如果不想猜，猜歌发起者和管理们可以输入 !g 来关闭。也可以等待它自行关闭。"
                ),
            )
        )

        data object Timeout: GuessReply(
            listOf(
                listOf(
                    "由于长时间无人操作，",
                    "由于不想被冷群，"
                ),
                listOf(
                    "猜歌已经自动结束。",
                    "猜歌已经被终止。"
                ),
                listOf("\n"),
                listOf(
                    "期待您的下次游玩！",
                    "多留意帮助文档的娱乐功能，好处多多哦。"
                ),
            )
        )

        data object Guessed: GuessReply(
            listOf(
                listOf(
                    "这张谱面已经被猜出来了，", "你想获取别人已经拿走的得分吗？", "不要重复猜了，"
                ),
                IDIOT
            )
        )

        data object ManyTips: GuessReply(
            listOf(
                listOf(
                    "已经给了你足够多的提示了，", "你需要我把答案直接喂给你吗？", "已经是我奶奶都能猜出来的难度了，"
                ),
                IDIOT
            )
        )

        data object Incorrect: GuessReply(
            listOf(
                SORRY,
                listOf(
                    "没有猜中歌曲呢。", "差一点点就能猜中了。", "好像不是这个答案呢。"
                )
            )
        )

        data object IncorrectMuch: GuessReply(
            listOf(
                listOf(
                    "开了这么多都猜不中，真是", "还是乖乖认输吧，", "要不要来点提示？"
                ),
                IDIOT
            )
        )

        data object Correct: GuessReply(
            listOf(
                listOf("您已经猜中以下谱面：", "其中一个答案已经展现：", "您已经参悟了其中的真谛：", "某个答案已为您揭晓：")
            )
        )

        data object Initializing: GuessReply(
            listOf(
                listOf("正在", "即将", "马上"),
                listOf("生成题目", "抓捕小猪", "构思野史", "思考人生", "刷 PP", "联系 peppy", "接入 OpenClaw", "擦皮鞋", "对抗神经网络"),
                listOf("...")
            )
        )

        data class Start(val username: String, val mode: OsuMode, val size: Int): GuessReply(
            listOf(
                listOf("猜歌开始！", "生成完成！", "游戏开始！"),
                listOf("\n"),
                listOf("当前将选用 $username 的 ${mode.fullName} 模式下，最好成绩中的 $size 张谱面。")
            )
        )

        data object MustGuess: GuessReply(
            listOf(
                listOf("你倒是猜啊？", "就算你求我，我也不会直接告诉你答案的。"),
                listOf("\n"),
                listOf("如果想放弃的话，", "如果想停止猜歌，听好了，${IDIOT.random()}"),
                listOf("\n"),
                listOf("只有猜歌发起者和管理们才可以 !g 停止猜歌。"),
                listOf("\n"),
                listOf("你也可以等待一会儿，猜歌会因为超时而结束。")
            )
        )

        data object StopCheck: GuessReply(
            listOf(
                listOf(
                    "您确定要结束当前的猜歌吗？",
                    "您真的要马上结束吗？"
                ),
                listOf(
                    "再考虑一下？",
                    "人家还想一起玩呢。"
                ),
                listOf("\n"),
                listOf(
                    "发送 OK 就能忍痛离开。",
                    "发送 OK 就可以停止了哦。"
                ),
            )
        )

        data object Bingo: GuessReply(
            listOf(
                listOf("一击即中！", "一\uD83D\uDC14\uD83D\uDC14中！", "这么厉害！？", "这么强？！", "？！文厶虽！？", "？！强强！？")
            )
        )

        companion object {
            // 后面这三个是猪
            val IDIOT: List<String> = listOf(
                "小笨蛋！", "杂鱼！", "小猪！", "\uD83D\uDC37！", "\uD83D\uDC16！", "\uD83D\uDC3D！"
            )

            val SORRY: List<String> = listOf(
                "很可惜，", "很抱歉，", "遗憾的是，"
            )

            // val PREFIX = listOf("", "诶，", "那个... ", "报告！", "呜呜，", "提醒一下：")
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

            if (game.nearingTimeout && !game.reminded.get()) {
                game.event.reply(GuessReply.NearingTimeout)
                game.reminded.set(true)
            }

            if (game.timeout) {
                iterator.remove()

                game.event.replyDone(game, GuessReply.Timeout)
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

        var reminded: AtomicBoolean = AtomicBoolean(false)

        val nearingTimeout: Boolean
            get() = lastPlayedTime.plusMinutes(5).isBefore(LocalDateTime.now())

        val timeout: Boolean
            get() = lastPlayedTime.plusMinutes(10).isBefore(LocalDateTime.now())

        fun update() {
            this.lastPlayedTime = LocalDateTime.now()
            this.reminded.set(false)
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

            val mask: Int get() = 1 shl this.ordinal

            companion object {
                val ALL_LOCKED: Int = entries.fold(0) { acc, hint -> acc or hint.mask }

                fun isLocked(mask: Int, hint: Hint): Boolean = (mask and hint.mask) != 0

                fun unlock(mask: Int, hint: Hint): Int = mask and hint.mask.inv()

                fun getRemaining(mask: Int): List<Hint> = entries.filter { isLocked(mask, it) }
            }
        }

        fun reward(index: Int): Int {
            val currentMask = decrypted[index]

            if (currentMask < 0) return 0

            val guessedHints = Hint.entries.size - Hint.getRemaining(currentMask).size
            val baseScore = 100 shr guessedHints

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
                throw TipsException(GuessReply.Guessed)
            }

            val remaining = Hint.getRemaining(currentMask)

            if (remaining.size <= 2) {
                throw TipsException(GuessReply.ManyTips)
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

            val protectedSymbols = setOf('(', ')', '[', ']', '{', '}', '（', '）', '【', '】', '.')

            for (i in charArray.indices) {
                val c = charArray[i]

                // 1. 如果是空格，原样保留
                if (c.isWhitespace()) continue

                // 2. 如果是括号类符号，原样保留
                if (protectedSymbols.contains(c)) continue

                // 3. 核心加密逻辑
                val std = DataUtil.getStandardisedString(c.toString())

                // 如果该字符还没被猜出来（不在已解锁集合中），则加密为 #
                if (std !in standardisedLetters) {
                    charArray[i] = '#'
                }
            }

            return String(charArray)
        }

        fun guess(text: String): List<Int> {
            update()

            val processedInput = text.getCore()

            val result = scores.mapIndexed { i, b ->
                if (decrypted[i] < 0) {
                    return@mapIndexed i to null
                }

                val s = b.beatmapset

                fun getProcessedSimilarity(title: String): Double {
                    val processedTitle = title.getCore()

                    return if (processedTitle.length <= 2 || processedInput.length <= 2) {
                        DataUtil.getStringSimilarity(title, text)
                    } else {
                        DataUtil.getStringSimilarity(processedTitle, processedInput)
                    }
                }

                val t = getProcessedSimilarity(s.title)
                val u = getProcessedSimilarity(s.titleUnicode)

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

        fun getHintForSpecialWords(input: String): String {
            val matchedMark = GUESS_SPECIAL_WORDS_REGEX.find(input)?.value?.trim('(', ')', '[', ']', ' ', '~', '-')
                ?: return "不要蒙混过关，${GuessReply.IDIOT.random()}"

            val indices = scores.mapIndexedNotNull { index, score ->
                val isDecrypted = decrypted[index] < 0
                if (isDecrypted) return@mapIndexedNotNull null

                val s = score.beatmapset
                // 检查标题或原名中是否包含玩家输入的那个标记
                val hasMark = s.title.contains(matchedMark, ignoreCase = true) ||
                        s.titleUnicode.contains(matchedMark, ignoreCase = true)

                if (hasMark) index + 1 else null
            }

            return if (indices.isNotEmpty()) {
                """
                    不要蒙混过关，${GuessReply.IDIOT.random()}
                    不过，这里确实有 ${indices.size} 个题目符合这条匹配。
                """.trimIndent()
            } else {
                """
                    不要蒙混过关，${GuessReply.IDIOT.random()}这里没有 $matchedMark。
                    ${listOf("大叔", "大姐姐").random()}下次还是换个词猜吧。
                """.trimIndent()
            }
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
                    val b = score.beatmap

                    if (decrypted[i] >= 0) {
                        mapOf(
                            "beatmapset" to mapOf(
                                "title" to encrypt(s.title),
                                "title_unicode" to encrypt(s.titleUnicode),
                                "artist" to encrypt(s.artist),
                                "artist_unicode" to encrypt(s.artistUnicode),
                                "creator" to encrypt(s.creator),

                                "covers" to s.covers,
                            ),

                            "beatmap" to mapOf(
                                "version" to encrypt(b.difficultyName),

                                "id" to b.beatmapID,
                                "difficulty_rating" to b.starRating,
                                "ranked" to b.ranked,
                                "status" to b.status,
                            )

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

                val hasSpecialWords = GUESS_SPECIAL_WORDS_REGEX.containsMatchIn(param.result)

                if (hasSpecialWords) {
                    val cores = param.result.getCore()

                    if (cores.length < 3) {
                        event.reply(game.getHintForSpecialWords(param.result))
                        return null
                    }
                }

                val result = game.guess(param.result)

                if (result.isEmpty()) {
                    if (game.revealedLetters.size <= 8) {
                        throw TipsException(GuessReply.Incorrect)
                    } else {
                        throw TipsException(GuessReply.IncorrectMuch)
                    }
                } else {
                    handleDecrypted(game, event, GuessReply.Correct.toString(), result)
                }
            }

            is GuessParam.GuessStartParam -> {
                if (game != null) {
                    throw TipsException("当前有正在进行中的猜歌，请等待此猜歌流程结束。")
                }

                event.reply(GuessReply.Initializing)

                // 之前已经猜过的歌曲 set ID
                val history = serviceCallStatisticsDao.getLast10BeatmapsetIDs(
                    groupID, "GUESS", LocalDateTime.now().minusMonths(3)
                )
                val frequencyMap = history.groupingBy { it }.eachCount()

                val selected = param.scores
                    .filter { score ->
                        val set = score.beatmapset

                        fun getCoreLength(text: String): Int {
                            val onlyCore = text.replace(GUESS_SPECIAL_WORDS_REGEX, "")
                            return onlyCore.replace(Regex("[^\\p{L}\\p{N}]"), "").length
                        }

                        val coreLen = getCoreLength(set.title)
                        val coreLenUnicode = getCoreLength(set.titleUnicode)

                        // 要求核心长度必须大于 2，否则这歌太难猜（全是标记）或者太容易误触
                        maxOf(coreLen, coreLenUnicode) >= 3
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

                event.replyGuess(game, GuessReply.Start(param.user.username, param.user.currentOsuMode, selected.size))
            }

            is GuessParam.GuessEndParam -> {
                if (game == null) {
                    throw TipsException("无法结束不存在的猜歌。")
                }

                if (game.event.sender.contactID != event.sender.contactID && !Permission.isGroupAdmin(event)) {
                    throw TipsException(GuessReply.MustGuess)
                }

                val receipt = event.reply(GuessReply.StopCheck)
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

                val revealed = game.reveal(param.char) { result ->
                    handleDecrypted(game, event, "以下谱面已自动解开：", result)
                }

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

        var rewards = 0
        var bingo = false

        result.forEachIndexed { i, r ->
            val b = game.scores[r]
            val set = b.beatmapset

            val title = if (game.unicode) set.titleUnicode else set.title

            val decrypt =
                "${set.artistUnicode} - ${set.titleUnicode}" + if (DataUtil.getStringSimilarity(set.title, set.titleUnicode, standardised = true) < SIMILARITY_THRESHOLD) {
                    " (${set.title})"
                } else {
                    ""
                }

            reply.append("#${r + 1}: $decrypt")

            if (i < result.size - 1) reply.append('\n')

            val isLuckyGuess = game.standardisedLetters.isEmpty() || (
                    title.replace(Regex("[^a-zA-Z0-9]"), "").all { c ->
                        val std = DataUtil.getStandardisedString(c.toString())

                        std !in game.standardisedLetters
                    })

            if (isLuckyGuess && game.rewards[r] >= 100) {
                bingo = true
            }

            rewards += game.rewards[r]
        }

        if (bingo) {
            reply.append("\n\n${GuessReply.Bingo}")
        } else {
            reply.append("\n")
        }

        reply.append("\n得分：${rewards}")

        event.replyGuess(game, reply.toString())

        KALEIDXSCOPE.launch {
            delay(500.milliseconds)
            if (game.trySettle()) {
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

    private fun MessageEvent.replyGuess(game: GuessGame, text: Any? = null) {

        runCatching {
            val image = imageService.getPanel(game.toMap(), "A8")

            if (text?.toString()?.isNotBlank() == true) {
                this.reply(image, text.toString())
            } else {
                this.reply(image)
            }

        }.onFailure {
            if (text?.toString()?.isNotBlank() == true) {
                this.reply(text)
            }

            KALEIDXSCOPE.launch {
                delay(500.milliseconds)
                this@replyGuess.reply(game.getTextMessage())
            }
        }
    }

    private fun MessageEvent.replyDone(
        game: GuessGame, text: Any? = "猜歌结束。", noGuess: Boolean = false
    ): ServiceCallStatistic? {
        val decryptedSetIDs = game.scores.zip(game.decrypted)
            .filter { it.second < 0 }
            .map { it.first.beatmapset.beatmapsetID }
            .toSet()

        game.decryptAll()

        if (noGuess && text != null) {
            game.event.reply(text)
        } else {
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

    override fun accept(
        event: MessageEvent,
        messageText: String
    ): GuessParam? {
        return null
    }

    override fun reply(
        event: MessageEvent,
        param: GuessParam
    ): MessageChain? {
        return null
    }
}