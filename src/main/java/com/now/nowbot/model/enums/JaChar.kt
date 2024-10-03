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
    SI("し", "シ", "si"),
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
    N("ん", "ン", "n"),
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
    VYO("ゔょ", "ヴョ", "vyo"),
    YA2("ゃ", "ャ", "a"),
    YU2("ゅ", "ュ", "u"),
    YO2("ょ", "ョ", "o"),

    ;
    companion object {
        @JvmStatic
        // 获取平假名片假名的罗马音，日文汉字就算了，那个要上 API
        fun getRomanized(japanese: String?): String {
            if (japanese.isNullOrEmpty()) {
                return ""
            }

            if (!containsJapanese(japanese)) {
                return japanese
            }

            val charArray = japanese.toCharArray()
            val sb = StringBuilder()

            outro@ for (i in charArray.indices) {
                val c = charArray[i]
                val n = if (i + 1 < charArray.size) charArray[i + 1] else null

                if (c == ' ') {
                    sb.append(' ')
                    continue@outro
                }


                if (n != null) {

                    // 促音 さっき -> Sakki (n = っ)
                    if (isSokuon(c)) {
                        for (j in JaChar.entries) {
                            if (isEqual(n, j)) {
                                sb.append(j.romanized.first())
                                continue@outro
                            }
                        }
                    }

                    // 拗音 みゃ -> mya (n = み)
                    if (isYouon(n)) {
                        for (j in JaChar.entries) {
                            if (isEqual(c.toString() + n.toString(), j)) {
                                sb.append(j.romanized.dropLast(1))
                                continue@outro
                            }
                        }
                    }
                }



                for (j in JaChar.entries) {
                    if (isEqual(c, j)) {
                        sb.append(j.romanized)
                        continue@outro
                    }
                }

                sb.append(c)
            }

            return sb.toString()
        }

        private fun containsJapanese(T: CharSequence): Boolean {
            return T.contains(Regex("[\u3040-\u30ff]"))
        }

        // 促音 さっき -> Sakki
        private fun isSokuon(char: Char): Boolean {
            return isEqual(char, TSU2)
        }

        // 拗音 みゃ -> mya
        private fun isYouon(char: Char): Boolean {
            return isEqual(char, YA2) || isEqual(char, YU2) || isEqual(char, YO2)
        }

        private fun isEqual(char: Char, jaChar: JaChar): Boolean {
            return isEqual(char.toString(), jaChar)
        }

        private fun isEqual(str: String, jaChar: JaChar): Boolean {
            return (str == jaChar.hiragana || str == jaChar.katakana)
        }
    }
}
