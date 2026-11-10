package com.now.nowbot.util

/**
 * 违禁词工具。
 *
 * 从 [com.now.nowbot.service.messageServiceImpl.DiceService] 拆出来的和谐逻辑：
 * - 整词命中 [RESTRICT_WORD] 的替换成 [WORD_MASK]；
 * - 单字命中 [RESTRICT_CHAR] 的替换成 [CHAR_MASK]；
 * - 命中 [OTTO] 这种地狱笑话的替换成空白。
 *
 * 所有会把用户输入原样发回群聊的地方都应该先过一遍 [harmonize]，
 * 否则机器人可能因为复读违禁词被平台禁言。
 */
object RestrictWordUtil {
    /** 整词命中违禁词后替换成的文本。 */
    const val WORD_MASK = "[和谐]"

    /** 单个违禁字命中后替换成的文本。 */
    const val CHAR_MASK = "○"

    /** 地狱笑话命中后替换成的文本，和拆出来之前的空格保持一致。 */
    private const val OTTO_MASK = "    "

    /** 需要整词替换的违禁词。 */
    private val RESTRICT_WORD = Regex(
        "[习習]近平|[习習]?总书记|主席|国家|政治|反动|反?共(产党)?|[国國]民[党黨]|天安[門门]|极[左右](主义)?|革命|(社会)?主义|自由|解放|中[華华]民[国國]|情趣|迪克|高潮|色[诱情欲色]|擦边|露出|[蛇射受授吞]精|潮喷|成人|性交|小?男娘|小?南梁|做爱|后入|药娘|怀孕|生殖器|寄吧|几把|鸡[鸡巴]|[精卵]子|[精爱]液|子宫|阴[茎蒂唇囊道]|[逼Bb阴吊叼批肛]毛|搞基|出?脚本|[Rr]-?18|18\\s?禁|LGBT"
    )

    /** 需要替换成 [CHAR_MASK] 的单个违禁字。 */
    private val RESTRICT_CHAR = Regex("[黨党吊批逼操肏肛杀穴屁萎猥]")

    /** 和电棍相关的地狱笑话。 */
    private val OTTO = Regex(
        "((电?[棍昆滚])|otto)\\s*的?\\s*((老?[木母]亲?)|([妈老]?妈)|(mom)|(mother))"
    )

    /** 已经和谐过的文本里会出现的占位符，用于判断要不要撤回消息。 */
    private val MASK_MARKER = Regex("○|\\[和谐]")

    /**
     * 文本里是否含有违禁词。
     *
     * 只做判断，不会改动原文；需要展示时请使用 [harmonize]。
     */
    fun contains(text: String?): Boolean {
        if (text.isNullOrEmpty()) return false

        return RESTRICT_WORD.containsMatchIn(text)
                || RESTRICT_CHAR.containsMatchIn(text)
                || OTTO.containsMatchIn(text)
    }

    /**
     * 把文本里的违禁词全部和谐掉。
     *
     * 替换顺序和拆出来之前一致：整词 -> 单字 -> 地狱笑话。
     *
     * @param text 未和谐的文本
     * @return 和谐后的文本；null 视为空串
     */
    fun harmonize(text: String?): String {
        if (text.isNullOrEmpty()) return text ?: ""

        return text.replace(RESTRICT_WORD, WORD_MASK)
            .replace(RESTRICT_CHAR, CHAR_MASK)
            .replace(OTTO, OTTO_MASK)
    }

    /**
     * 文本里是否含有和谐后的占位符（[WORD_MASK] 或 [CHAR_MASK]）。
     *
     * 用来判断一条已经发出去的消息是不是被和谐过，需要撤回时使用。
     */
    fun isHarmonized(text: String?): Boolean {
        if (text.isNullOrEmpty()) return false

        return MASK_MARKER.containsMatchIn(text)
    }
}
