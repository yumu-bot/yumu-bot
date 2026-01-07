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
        // 获取平假名片假名的罗马音，日文汉字就算了，那个要上 API
        fun getRomanized(japanese: String?): String {
            if (japanese.isNullOrEmpty()) {
                return ""
            }

            if (!hasJapanese(japanese)) {
                return japanese
            }

            val charArray = japanese.toCharArray()
            val sb = StringBuilder()

            outro@ for (i in charArray.indices) {
                val now = charArray[i]
                val after = if (i + 1 < charArray.size) charArray[i + 1] else null
                val before = if (i > 0) charArray[i - 1] else null

                if (now == ' ') {
                    sb.append(' ')
                    continue@outro
                }

                // 长音 えー -> ee
                if (isChouon(now) && before != null) {
                    for (j in JaChar.entries) {
                        if (isEqual(before, j)) {
                            sb.append(j.romanized.last())
                            continue@outro
                        }
                    }
                }

                // 促音 さっき -> Sakki (c = っ)
                if (isSokuon(now)) {
                    if (after != null && hasJapanese(after)) {

                        // は (ha) 行前有拗音的话，有音变，要变成 ぱ (pa) 行。放在下一轮处理。
                        if (isEqual(after, listOf(HA, HI, FU, HE, HO))) {
                            continue@outro
                        }

                        for (j in JaChar.entries) {
                            if (isEqual(after, j)) {
                                sb.append(j.romanized.first())
                                continue@outro
                            }
                        }
                    } else if (before != null) {
                        // 比如 んっあっあっ，此时最后一个促音应该跟着前面 a。
                        for (j in JaChar.entries) {
                            if (isEqual(before, j)) {
                                sb.append(j.romanized.last())
                                continue@outro
                            }
                        }
                    }
                }

                // は (ha) 行前有拗音的话，有音变，要变成 ぱ (pa) 行。在这里处理。
                if (before != null && isSokuon(before) && isEqual(now, listOf(HA, HI, FU, HE, HO))) {
                    for (j in listOf(HA, HI, FU, HE, HO)) {
                        if (isEqual(now, j)) {
                            sb.append((j.romanized.first() + j.romanized).replace('h', 'p'))
                            continue@outro
                        }
                    }
                }

                // 拗音 みゃ -> mya (c = み)
                if (after != null && isYouon(after)) {
                    for (j in JaChar.entries) {
                        if (isEqual(now.toString() + after.toString(), j)) {
                            sb.append(j.romanized.dropLast(1))
                            continue@outro
                        }
                    }
                }

                for (j in JaChar.entries) {
                    if (isEqual(now, j)) {
                        sb.append(j.romanized)
                        continue@outro
                    }
                }

                sb.append(now)
            }

            return sb.toString()
        }

        private val JAPANESE_RANGES = listOf(
            0x3040..0x309F,  // 平假名
            0x30A0..0x30FF,  // 片假名
            0x31F0..0x31FF,  // 片假名扩展（小写假名等）
            0xFF66..0xFF9F,  // 半角片假名
            //0x4E00..0x9FFF,  // CJK统一汉字（包含日文汉字）
            //0x3400..0x4DBF,  // CJK扩展A
            //0x3000..0x303F,  // CJK符号和标点
            0xFF00..0xFFEF,  // 全角ASCII及全角标点
        )

        private fun hasJapanese(charSequence: CharSequence): Boolean {
            // 遍历检查每个字符
            for (i in 0 until charSequence.length) {
                val code = charSequence[i].code
                if (JAPANESE_RANGES.any { code in it }) {
                    return true
                }
            }
            return false
        }

        private fun hasJapanese(char: Char): Boolean {
            return JAPANESE_RANGES.any { char.code in it }
        }

        // 促音 さっき -> Sakki
        private fun isSokuon(char: Char): Boolean {
            return isEqual(char, TSU2)
        }

        // 拗音 みゃ -> mya
        private fun isYouon(char: Char): Boolean {
            return isEqual(char, listOf(YA2, YI2, YU2, YE2, YO2))
        }

        // 长音 ええ -> ee
        private fun isChouon(char: Char): Boolean {
            return char == 'ー'
        }

        private fun isEqual(char: Char, jaChar: JaChar): Boolean {
            if (jaChar.hiragana.length > 1 || jaChar.katakana.length > 1) return false

            return (jaChar.hiragana.contains(char) || jaChar.katakana.contains(char))
        }

        private fun isEqual(char: Char, jaChars: List<JaChar>): Boolean {
            val str = char.toString()

            return jaChars.any { ja ->
                isEqual(str, ja)
            }
        }

        private fun isEqual(str: String, jaChar: JaChar): Boolean {
            return (str == jaChar.hiragana || str == jaChar.katakana)
        }
    }
}
