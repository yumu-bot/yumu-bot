package com.now.nowbot.model.enums

enum class MaiCharter(val japaneses: List<String>, val alias: String) {

    TATAMI_KAESHII(listOf("畳返し"), "榻榻米"),

    NYAIN(listOf("ニャイン", "nyain"), "二爷"),

    FUMEN_100(listOf("譜面-100号", "譜百", "100"), "100号"),

    CHAN_DP_KAIDEN(listOf("チャン@DP皆伝", "チャン", "舞舞10年ズ", "DP", "CHAN"), "DP皆传"),

    MAI_STAR(listOf("mai-Star", "maistar"), "mai-Star"),

    HAPPY(listOf("はっぴー", "原田ひろゆき", "緑風 犬三郎", "ユビキリ", "みんなでマイマイマー", "いぬっくま", "(十,3、了ﾅﾆ", "～ファイナル～", "ッピー", "たかなっぴー", "happy"), "嗨皮"),

    BOUU_S_SHI(listOf("某S氏", "某S"), "某S氏"),

    ROSHIE_PENGUIN(listOf("ロシェ@ペンギン", "ロシェ", "penguin"), "企鹅"),

    // 可能就是企鹅
    AMAKUCHI_GINGER(listOf("あまくちジンジャー", "あまくち", "project raputa", "EL DiABLO", "甜口姜"), "甜姜"),

    JACK(listOf("Jack", "ハートのジャック", "“H”ack", "JAQ", "hack"), "Jack"),

    TECHNO_KITCHEN(listOf("Techno Kitchen", "techno", "kitchen"), "科技厨房"),

    RION(listOf("rioN", "しろいろ", "shiroiro"), "rioN"),

    REVO_LC(listOf("Revo@LC"), "Revo"),

    PEACH_NEKO(listOf("ぴちネコ", "ロシアンブラック", "CAT", "チェシャ猫", "ネコトリ"), "桃子猫"),

    KISARAGI_YUKARI(listOf("如月 ゆかり", "yukari", "ゆかり"), "如月"),

    TAMAGO_TOFU(listOf("玉子豆腐", "tamago", "玉子", "豆腐"), "玉子豆腐"),

    MOON_STRIX(listOf("Moon Strix"), "月枭"),

    LABILABI(listOf("LabiLabi"), "Labi"),

    TAKAHANASHI(listOf("小鳥遊さん", "Phoenix"), "小鸟游"),

    MONO_CLOCK(listOf("ものくろっく", "一ノ瀬 リズ", "ものく"), "单钟"),

    SUKIYAKI_BUGYOU(listOf("すきやき奉行", "Sukiyaki"), "奉行"),

    SAFUATA(listOf("サファ太", "-ZONE- SaFaRi", "-ZONE-", "さふぁた", "safata", "Safari", "サファ", "沙发", "太", "翠楼屋", "翡翠マナ"), "沙发太"),

    MISUTORE_TEIN(listOf("ミストルティン"), "寄生植物"),

    // 这个是大国奏音
    HANAHI_SHOKUNIN(listOf("華火職人", "華火", "HANABI", "HAN∀BI", "大国奏音", "ﾚよ†ょ／Ｕヽ” ┠", "华火"), "华火职人"),

    SHICHIMI_HERTZ(listOf("シチミヘルツ", "ヘルツ", "7.3Hz", "7.3GHz", "7.3", ".GHz", "SHICHIMI", "しちみ", "七味", "Hz", "シチミ"), "七三赫兹"),

    USAGI_LAUNDRY(listOf("うさぎランドリー", "兔子", "洗衣店"), "兔子洗衣店"),

    GUNJOU_RIKORISU(listOf("群青リコリス", "Licorice Gunjyo", "りこりす", "群青"), "群青莉可丽丝"),

    KTM(listOf("KTM"), "KTM"),

    SUMIDAGAWA_SEIJIN(listOf("隅田川星人", "The ALiEN", "隅田川", "星人"), "隅田川星人"),

    AMA_RIRISU(listOf("アマリリス", "莉莉丝"), "阿玛莉莉丝"),

    AMI_NOHABAKIRI(listOf("アミノハバキリ", "habakiri"), "天羽羽斩"),

    RED_ARROW(listOf("Redarrow", "R.Arrow"), "红箭"),

    MISO_KATSU_SAMURAI(listOf("みそかつ侍", "miso", "miso katsu"), "味噌胜武士"),

    JYAKO_LEMON(listOf("じゃこレモン", "檸檬", "lemon"), "柠檬"),

    KAMABOKO_KUN(listOf("カマボコ君", "ボコっくま", "ボコ", "boko"), "鱼糕君"),

    PG_NAGAKAWA(listOf("PG-NAKAGAWA", "nagakawa"), "长川"),

    MELON_POP(listOf("メロンポップ", "ポップ", "melon", "pop"), "西瓜汽水"),

    YUU(listOf("佑", "project raputa", "yuu"), "佑"),

    KYOU_MURIN(listOf("きょむりん", "murin"), "木林"),

    HATO_HOLDER(listOf("鳩ホルダー", "鳩", "The Dove", "holder", "hato"), "鸠"),

    RUSHIERU(listOf("Luxizhel", "Luxi", "zhel"), "泸溪河"),

    RINTARO_SOMA(listOf("rintaro soma", "rintaro"), "林太郎"),

    RINGO_FULL_SET(listOf("りんご Full Set", "ringo"), "苹果"),

    MIZORE_YANAGI(listOf("みぞれヤナギ", "mizore", "yanagi"), "雨夹雪柳"),

    MINIMUM_LIGHT(listOf("ミニミライト", "Twinrook", "minimum", "light"), "微光"),

    KARAMERU_GURUN(listOf("からめる & ぐるん"), "转转"),

    COLLAB(listOf("合作だよ", "collab"), "合作"),

    OTHERS(listOf("-"), "其他"),

    ;

    companion object {
        fun getCharter(charter: String?): List<String> {
            if (charter.isNullOrEmpty()) {
                return listOf(COLLAB.alias)
            } else if (charter.contains("はっぴー respects for 某S氏")) {
                return listOf(HAPPY.alias)
            }

            val result = MaiCharter.entries.mapNotNull { entry ->
                val contains = entry.japaneses.map { ja ->
                    charter.contains(ja, ignoreCase = true)
                }.contains(element = true) || charter.contains(entry.alias, ignoreCase = true)

                if (contains && entry != OTHERS) {
                    entry
                } else {
                    null
                }
            }.toSet().map { it.alias }

            return result.ifEmpty {
                listOf(charter)
            }
        }
    }
}