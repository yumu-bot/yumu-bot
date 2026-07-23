package com.now.nowbot.model.enums

enum class JaChar(val hiragana: String, val katakana: String, val romanized: String) {
    A("あ", "ア", "a"),
    A2("ぁ", "ァ", "a"),
    I("い", "イ", "i"),
    I2("ぃ", "ィ", "i"),
    U("う", "ウ", "u"),
    U2("ぅ", "ゥ", "u"),
    E("え", "エ", "e"),
    E2("ぇ", "ェ", "e"),
    O("お", "オ", "o"),
    O2("ぉ", "ォ", "o"),
    KA("か", "カ", "ka"),
    KI("き", "キ", "ki"),
    KU("く", "ク", "ku"),
    KE("け", "ケ", "ke"),
    KO("こ", "コ", "ko"),
    SA("さ", "サ", "sa"),
    SHI("し", "シ", "shi"),
    SU("す", "ス", "su"),
    SE("せ", "セ", "se"),
    SO("そ", "ソ", "so"),
    TA("た", "タ", "ta"),
    CHI("ち", "チ", "chi"),
    TSU("つ", "ツ", "tsu"),
    TSU2("っ", "ッ", "tsu"), // 促音
    TE("て", "テ", "te"),
    TO("と", "ト", "to"),
    NA("な", "ナ", "na"),
    NI("に", "ニ", "ni"),
    NU("ぬ", "ヌ", "nu"),
    NE("ね", "ネ", "ne"),
    NO("の", "ノ", "no"),
    HA("は", "ハ", "ha"),
    HI("ひ", "ヒ", "he"),
    FU("ふ", "フ", "fu"),
    HE("へ", "ヘ", "he"),
    HO("ほ", "ホ", "ho"),
    MA("ま", "マ", "ma"),
    MI("み", "ミ", "mi"),
    MU("む", "ム", "mu"),
    ME("め", "メ", "me"),
    MO("も", "モ", "mo"),
    YA("や", "ヤ", "ya"),
    YU("ゆ", "ユ", "yu"),
    YO("よ", "ヨ", "yo"),
    RA("ら", "ラ", "ra"),
    RI("り", "リ", "ri"),
    RU("る", "ル", "ru"),
    RE("れ", "レ", "re"),
    RO("ろ", "ロ", "ro"),
    WA("わ", "ワ", "wa"),
    WI("ゐ", "ウィ", "wi"),
    WI2("ゐ", "ウィ", "wi"),
    WE("ゑ", "ヰ", "we"),
    WE2("ゑ", "ヱ", "we"),
    WO("を", "ヲ", "wo"),
    GA("が", "ガ", "ga"),
    GI("ぎ", "ギ", "gi"),
    GU("ぐ", "グ", "gu"),
    GE("げ", "ゲ", "ge"),
    GO("ご", "ゴ", "go"),
    ZA("ざ", "ザ", "za"),
    JI("じ", "ジ", "ji"),
    ZU("ず", "ズ", "zu"),
    ZE("ぜ", "ゼ", "ze"),
    ZO("ぞ", "ゾ", "zo"),
    DA("だ", "ダ", "da"),
    DJI("ぢ", "ヂ", "dji"),
    DZU("づ", "ヅ", "du"),
    DE("で", "デ", "de"),
    DO("ど", "ド", "do"),
    BA("ば", "バ", "ba"),
    BI("び", "ビ", "bi"),
    BU("ぶ", "ブ", "bu"),
    BE("べ", "ベ", "be"),
    BO("ぼ", "ボ", "bo"),
    PA("ぱ", "パ", "pa"),
    PI("ぴ", "ピ", "pi"),
    PU("ぷ", "プ", "pu"),
    PE("ぺ", "ペ", "pe"),
    PO("ぽ", "ポ", "po"),

    N("ん", "ン", "n"),
    V("ゔ", "ヴ", "v"),

    // 拗音
    KYA("きゃ", "キャ", "kya"),
    KYU("きゅ", "キュ", "kyu"),
    KYO("きょ", "キョ", "kyo"),
    GYA("ぎゃ", "ギャ", "gya"),
    GYU("ぎゅ", "ギュ", "gyu"),
    GYO("ぎょ", "ギョ", "gyo"),
    SYA("しゃ", "シャ", "sya"),
    SYU("しゅ", "シュ", "syu"),
    SYO("しょ", "ショ", "syo"),
    JA("じゃ", "ジャ", "ja"),
    JU("じゅ", "ジュ", "ju"),
    JO("じょ", "ジョ", "jo"),
    JA2("じゃ", "ヂャ", "ja"),
    JU2("じゅ", "ヂュ", "ju"),
    JO2("じょ", "ヂョ", "jo"),
    CHA("ちゃ", "チャ", "cha"),
    CHU("ちゅ", "チュ", "chu"),
    CHO("ちょ", "チョ", "cho"),
    NYA("にゃ", "ニャ", "nya"),
    NYU("にゅ", "ニュ", "nyu"),
    NYO("にょ", "ニョ", "nyo"),
    HYA("ひゃ", "ヒャ", "hya"),
    HYU("ひゅ", "ヒュ", "hyu"),
    HYO("ひょ", "ヒョ", "hyo"),
    BYA("ぴゃ", "ビャ", "bya"),
    BYU("ぴゅ", "ビュ", "byu"),
    BYO("ぴょ", "ビョ", "byo"),
    PYA("ぴゃ", "ピャ", "pya"),
    PYU("ぴゅ", "ピュ", "pyu"),
    PYO("ぴょ", "ピョ", "pyo"),
    MYA("みゃ", "ミャ", "mya"),
    MYU("みゅ", "ミュ", "myu"),
    MYO("みょ", "ミョ", "myo"),
    RYA("りゃ", "リャ", "rya"),
    RYU("りゅ", "リュ", "ryu"),
    RYO("りょ", "リョ", "ryo"),
    VYA("ゔゃ", "ヴャ", "vya"),
    VYU("ゔゅ", "ヴュ", "vyu"),
    VYE("ゔぇ", "ヴェ", "vye"),
    VYO("ゔょ", "ヴョ", "vyo"),

    YA2("ゃ", "ャ", "a"),
    YI2("ぃ", "ィ", "i"),
    YU2("ゅ", "ュ", "u"),
    YE2("ぇ", "ェ", "e"),
    YO2("ょ", "ョ", "o"),

    ;
    companion object {
        private val Loong: Char = 'ー'

        // 单假名查找表: CharCode -> JaChar
        private val SINGLE_LOOKUP = Array<JaChar?>(65536) { null }

        // 拗音查找表: (前字Code shl 16 or 后字Code) -> 罗马音 (例如: "き" + "ゃ" -> "kya")
        private val YOUON_LOOKUP = HashMap<Int, String>()

        // 促音字符 Set (っ 和 ッ)
        private val SOKUON_CHARS = CharArray(65536)

        // 拗音小字 Set (ゃ ぃ ゅ ぇ ょ / ャ ィ ュ ェ ョ)
        private val YOUON_SMALL_CHARS = BooleanArray(65536)

        // は行假名 CharCode 集合 (用于促音+は行的音变判断: は ひ ふ へ ほ / ハ ヒ フ ヘ ホ)
        private val HA_ROW_CODES = IntArray(65536)

        init {
            for (ja in entries) {
                // 单字符假名注册
                if (ja.hiragana.length == 1) {
                    SINGLE_LOOKUP[ja.hiragana[0].code] = ja
                }
                if (ja.katakana.length == 1) {
                    SINGLE_LOOKUP[ja.katakana[0].code] = ja
                }

                // 拗音组合假名注册 (如 hiragana/katakana 长度为2，如 "みゃ")
                if (ja.hiragana.length == 2) {
                    val key = (ja.hiragana[0].code shl 16) or ja.hiragana[1].code
                    YOUON_LOOKUP[key] = ja.romanized
                }
                if (ja.katakana.length == 2) {
                    val key = (ja.katakana[0].code shl 16) or ja.katakana[1].code
                    YOUON_LOOKUP[key] = ja.romanized
                }
            }

            // 标记促音
            SOKUON_CHARS[TSU2.hiragana[0].code] = TSU2.hiragana[0]
            SOKUON_CHARS[TSU2.katakana[0].code] = TSU2.katakana[0]

            // 标记小写拗音
            listOf(YA2, YI2, YU2, YE2, YO2).flatMap { listOf(it.hiragana, it.katakana) }.forEach {
                YOUON_SMALL_CHARS[it[0].code] = true
            }

            // 标记 HA 行假名
            listOf(HA, HI, FU, HE, HO).flatMap { listOf(it.hiragana, it.katakana) }.forEach {
                HA_ROW_CODES[it[0].code] = 1
            }
        }

        /**
         * 将 CharSequence 中的假名转换为罗马音追加到 target StringBuilder
         */
        fun CharSequence.appendRomanizedJapaneseTo(target: StringBuilder) {
            val len = this.length
            var i = 0

            while (i < len) {
                val now = this[i]
                val nowCode = now.code
                val next = if (i + 1 < len) this[i + 1] else null
                val prev = if (i > 0) this[i - 1] else null

                // 1. 空格直接追加
                if (now == ' ') {
                    target.append(' ')
                    i++
                    continue
                }

                // 2. 优先检查：是否与下一个字符构成【拗音】(如 みゃ -> mya)
                if (next != null) {
                    val youonKey = (nowCode shl 16) or next.code
                    val youonRomanized = YOUON_LOOKUP[youonKey]
                    if (youonRomanized != null) {
                        target.append(youonRomanized)
                        i += 2 // 消费掉两个字符！
                        continue
                    }
                }

                // 3. 检查：长音符号『ー』 (如 えー -> ee)
                if (now == Loong && prev != null) {
                    val prevJa = SINGLE_LOOKUP[prev.code]
                    if (prevJa != null && prevJa.romanized.isNotEmpty()) {
                        target.append(prevJa.romanized.last())
                        i++
                        continue
                    }
                }

                // 4. 检查：促音『っ』/『ッ』(如 さっき -> sakki)
                if (SOKUON_CHARS[nowCode] != '\u0000') {
                    var handled = false
                    if (next != null) {
                        val nextCode = next.code
                        // 特殊音变：促音 + は行 -> 音变为 ぱ行 (っは -> ppa)
                        if (HA_ROW_CODES[nextCode] == 1) {
                            val nextJa = SINGLE_LOOKUP[nextCode]
                            if (nextJa != null) {
                                // 把 ha/hi/fu/he/ho 的 h 换成 p，并在前面加 p
                                val pSound = nextJa.romanized.replace('h', 'p').replace('f', 'p')
                                target.append('p').append(pSound)
                                i += 2 // 消费促音和后面的は行字符
                                continue
                            }
                        }

                        // 普通促音：取后一个假名罗马字的首字母 (如 き -> k -> 追加 k)
                        val nextJa = SINGLE_LOOKUP[nextCode]
                        if (nextJa != null && nextJa.romanized.isNotEmpty()) {
                            target.append(nextJa.romanized.first())
                            handled = true
                        }
                    }

                    // 兜底促音：如 "んっあっ"，后无跟随假名，取前一个元音末尾字母
                    if (!handled && prev != null) {
                        val prevJa = SINGLE_LOOKUP[prev.code]
                        if (prevJa != null && prevJa.romanized.isNotEmpty()) {
                            target.append(prevJa.romanized.last())
                            handled = true
                        }
                    }

                    if (handled) {
                        i++
                        continue
                    }
                }

                // 5. 普通单字符假名查表
                val ja = SINGLE_LOOKUP[nowCode]
                if (ja != null) {
                    target.append(ja.romanized)
                } else {
                    target.append(now) // 非假名原样追加
                }

                i++
            }
        }

        fun getRomanized(japanese: String?): String {
            if (japanese.isNullOrEmpty()) return ""
            val sb = StringBuilder(japanese.length * 2)
            japanese.appendRomanizedJapaneseTo(sb)
            return sb.toString()
        }
    }
}
