package com.now.nowbot.image.maimai

import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.randomBanner
import com.now.nowbot.image.util.ColorArray
import java.util.Locale
import kotlin.math.floor

class MaiAssets(private val assets: ImageAssets) {
    fun local(name: String): String? = assets.local(name)

    fun remote(url: String?): String? = assets.remote(url)

    fun cover(songID: Long): String? {
        val id = songID.coerceAtLeast(0)
        local("Maimai/Cover/${coverPathID(id)}.png")?.let { return it }

        val raw = rawCoverID(id)
        if (raw > 0) {
            remote("https://assets2.lxns.net/maimai/jacket/$raw.png")?.let { return it }
        }
        return local("Maimai/default.png")
    }

    fun plate(plateID: Int): String? {
        if (plateID <= 0) return null
        local("Maimai/Plate/$plateID.png")?.let { return it }
        remote("https://assets2.lxns.net/maimai/plate/$plateID.png")?.let { return it }
        return local("Maimai/Plate/0.png")
    }

    fun ratingPlate(rating: Int): String? = local(ratingPlateFile(rating))

    fun ratingDigit(digit: Char): String? = local("Maimai/UI/UI_NUM_Drating_$digit.png")

    fun ratingBackground(rating: Int): String? = local(ratingBackgroundFile(rating))

    fun rankBackground(rank: String?): String? = local(rankBackgroundFile(rank))

    fun rankImage(rank: String?): String? = local(rankImageFile(rank))

    fun typeImage(type: String?): String? = typeFile(type)?.let { local(it) }

    fun versionBackground(version: String?): String? = versionFile(version)?.let { local(it) }

    fun comboIcon(combo: String?): String? = local(comboIconFile(combo))

    fun syncIcon(sync: String?): String? = local(syncIconFile(sync))

    fun chunithmCover(songID: Long): String? {
        val id = songID.coerceAtLeast(0)
        local("Chunithm/Cover/$id.png")?.let { return it }
        if (id > 0) {
            remote("https://assets2.lxns.net/chunithm/jacket/$id.png")?.let { return it }
        }
        return local("Chunithm/default.png")
    }

    fun chunithmRankBackground(score: Int): String? = local(chunithmRankBackgroundFile(score))

    fun chunithmRankImage(score: Int): String? =
        local("Chunithm/object-score-${chunithmRank(score)}2.png")

    fun chunithmRatingBackground(rating: Double): String? =
        local(chunithmRatingBackgroundFile(rating))

    fun chunithmChainIcon(combo: String?): String? {
        val name = when (combo) {
            "fullcombo" -> "fullcombo"
            "fullchain" -> "fullchain"
            "alljustice" -> "alljustice"
            else -> return null
        }
        return local("Chunithm/object-icon-chain-$name.png")
    }

    fun overlayFoil(): String? = local("avatar-foil.png")

    fun randomBanner(index: Int? = null): String? = assets.randomBanner("maimai", index)


    fun defaultCover(): String? = local("Maimai/default.png")

    fun avatarGuest(): String? = local("Maimai/avatar-guest.png")

    fun favoriteIcon(): String? = local("object-beatmap-favorite.png")

    fun playcountIcon(): String? = local("object-beatmap-playcount.png")

    fun versusBackground(): String? = local("Maimai/background-versus.jpg")

    companion object {
        private val ratingThresholds = intArrayOf(
            1000, 2000, 4000, 7000, 10000, 12000, 13000, 14000, 14500, 15000, 16000,
        )
        private val ratingBackgrounds = listOf(
            "F", "B", "A", "SP", "D", "C", "S", "SH", "X", "XH", "PF", "PP",
        )
        private val rankBackgrounds = mapOf(
            "sssp" to "object-score-backimage-PF.webp",
            "sss" to "object-score-backimage-SSS.webp",
            "ssp" to "object-score-backimage-X.webp",
            "ss" to "object-score-backimage-X.webp",
            "sp" to "object-score-backimage-S.webp",
            "s" to "object-score-backimage-S.webp",
            "aaa" to "object-score-backimage-D.webp",
            "aa" to "object-score-backimage-D.webp",
            "a" to "object-score-backimage-D.webp",
            "bbb" to "object-score-backimage-B.webp",
            "bb" to "object-score-backimage-B.webp",
            "b" to "object-score-backimage-B.webp",
            "c" to "object-score-backimage-A.webp",
            "d" to "object-score-backimage-F.webp",
        )
        private val versionBackgrounds = mapOf(
            "maimai" to "object-version-maimai.png",
            "maimai PLUS" to "object-version-maimai-plus.png",
            "maimai GreeN" to "object-version-maimai-green.png",
            "maimai GreeN PLUS" to "object-version-maimai-green-plus.png",
            "maimai ORANGE" to "object-version-maimai-orange.png",
            "maimai ORANGE PLUS" to "object-version-maimai-orange-plus.png",
            "maimai PiNK" to "object-version-maimai-pink.png",
            "maimai PiNK PLUS" to "object-version-maimai-pink-plus.png",
            "maimai MURASAKi" to "object-version-maimai-murasaki.png",
            "maimai MURASAKi PLUS" to "object-version-maimai-murasaki-plus.png",
            "maimai MiLK" to "object-version-maimai-milk.png",
            "maimai MiLK PLUS" to "object-version-maimai-milk-plus.png",
            "MiLK PLUS" to "object-version-maimai-milk-plus.png",
            "maimai FiNALE" to "object-version-maimai-finale.png",
            "maimai でらっくす" to "object-version-maimai-dx.png",
            "maimai でらっくす PLUS" to "object-version-maimai-dx-plus.png",
            "maimai でらっくす Splash" to "object-version-maimai-dx-splash.png",
            "maimai でらっくす Splash PLUS" to "object-version-maimai-dx-splash-plus.png",
            "maimai でらっくす UNiVERSE" to "object-version-maimai-dx-universe.png",
            "maimai でらっくす UNiVERSE PLUS" to "object-version-maimai-dx-universe-plus.png",
            "maimai でらっくす FESTiVAL" to "object-version-maimai-dx-festival.png",
            "maimai でらっくす FESTiVAL PLUS" to "object-version-maimai-dx-festival-plus.png",
            "maimai でらっくす BUDDiES" to "object-version-maimai-dx-buddies.png",
            "maimai でらっくす BUDDiES PLUS" to "object-version-maimai-dx-buddies-plus.png",
            "maimai でらっくす PRiSM" to "object-version-maimai-dx-prism.png",
            "maimai でらっくす PRiSM PLUS" to "object-version-maimai-dx-prism-plus.png",
            "maimai でらっくす CiRCLE" to "object-version-maimai-dx-circle.png",
            "maimai でらっくす CiRCLE PLUS" to "object-version-maimai-dx-circle-plus.png",
            "maimai でらっくす MAGiCAL" to "object-version-maimai-dx-magical.png",
            "maimai でらっくす MAGiCAL PLUS" to "object-version-maimai-dx-magical-plus.png",
        )
        private val comboIcons = mapOf(
            "fc" to "fullcombo",
            "fcp" to "fullcomboplus",
            "ap" to "allperfect",
            "app" to "allperfectplus",
        )
        private val syncIcons = mapOf(
            "sync" to "sync",
            "fs" to "fullsync",
            "fsp" to "fullsyncplus",
            "fsd" to "fullsyncdx",
            "fsdp" to "fullsyncdxplus",
        )
        private val difficultyColors = listOf(
            "#009944", "#fff100", "#d32f2f", "#9922ee", "#f7d8fe",
        )

        fun coverPathID(songID: Long): String {
            val id = songID.coerceAtLeast(0)
            return if (id >= 10000) {
                (id % 10000 + 10000).toString()
            } else {
                id.toString().padStart(5, '0')
            }
        }

        fun rawCoverID(songID: Long): Long {
            val id = songID.coerceAtLeast(0)
            return if (id >= 10000) id % 10000 else id
        }

        fun ratingPlateFile(rating: Int): String {
            val index = ratingThresholds.indexOfFirst { rating < it }
            val background = if (index == -1) "11" else (index + 1).toString().padStart(2, '0')
            return "Maimai/UI/UI_CMN_DXRating_$background.png"
        }

        fun ratingBackgroundFile(rating: Int): String {
            val index = ratingThresholds.indexOfFirst { rating < it }
            val suffix = if (index == -1) "PP" else ratingBackgrounds[index]
            return "object-score-backimage-$suffix.webp"
        }

        fun rankBackgroundFile(rank: String?): String {
            val key = rank?.lowercase(Locale.ROOT)
            return key?.let { rankBackgrounds[it] } ?: "object-score-backimage-F.webp"
        }

        fun rankImageFile(rank: String?): String {
            val value = rank?.lowercase(Locale.ROOT)?.takeIf { it.isNotBlank() } ?: "d"
            return "Maimai/object-score-${value}2.png"
        }

        fun typeFile(type: String?): String? = when (type?.uppercase(Locale.ROOT)) {
            "DX" -> "Maimai/object-type-deluxe.png"
            "SD" -> "Maimai/object-type-standard.png"
            else -> null
        }

        fun versionFile(version: String?): String? {
            val file = version?.let { versionBackgrounds[it] } ?: return null
            return "Maimai/$file"
        }

        fun category(genre: String?): String {
            val value = genre.orEmpty()
            return when (value.trim()) {
                "东方Project", "東方Project" -> "Touhou Project"
                "maimai", "舞萌" -> "maimai"
                "niconico & VOCALOID", "niconicoボーカロイド" -> "niconico & VOCALOID"
                "流行&动漫", "POPSアニメ" -> "POPS & ANIME"
                "其他游戏", "ゲームバラエティ" -> "GAME & VARIETY"
                "音击&中二节奏", "オンゲキCHUNITHM" -> "Ongeki & CHUNITHM"
                "宴会場", "宴会场" -> "U * TA * GE"
                else -> value
            }
        }

        fun comboIconFile(combo: String?): String {
            val name = combo?.lowercase(Locale.ROOT)?.let { comboIcons[it] } ?: "clear"
            return "Maimai/object-icon-combo-$name.png"
        }

        fun syncIconFile(sync: String?): String {
            val name = sync?.lowercase(Locale.ROOT)?.let { syncIcons[it] } ?: "solo"
            return "Maimai/object-icon-sync-$name.png"
        }

        fun difficultyColor(index: Int, isUtage: Boolean): String {
            if (isUtage) return "#d46da1"
            return difficultyColors.getOrElse(index) { "#d46da1" }
        }

        fun starColor(star: Int): String = when {
            star >= 5 -> "#fbf365"
            star >= 3 -> "#ffb84d"
            star >= 1 -> "#6fc576"
            else -> "none"
        }

        fun starLevel(dxScore: Int, dxMax: Int): Int {
            if (dxMax <= 0) return 0
            val div = dxScore.toDouble() / dxMax
            return when {
                div >= 0.97 -> 5
                div >= 0.95 -> 4
                div >= 0.93 -> 3
                div >= 0.9 -> 2
                div >= 0.85 -> 1
                else -> 0
            }
        }

        fun maximumRating(ds: Double): Int = floor(ds * 1.005 * 22.4).toInt()

        fun isMaximumRating(rating: Int, ds: Double): Boolean =
            rating > 0 && rating >= maximumRating(ds)

        fun maimaiRankFromAchievements(achievements: Double): String {
            val rate = minOf(maxOf(achievements, 0.0), 101.0)
            return when {
                rate >= 100.5 -> "sssp"
                rate >= 100.0 -> "sss"
                rate >= 99.0 -> "ssp"
                rate >= 98.0 -> "ss"
                rate >= 97.0 -> "sp"
                rate >= 94.0 -> "s"
                rate >= 90.0 -> "aaa"
                rate >= 85.0 -> "aa"
                rate >= 80.0 -> "a"
                rate >= 75.0 -> "bbb"
                rate >= 70.0 -> "bb"
                rate >= 65.0 -> "b"
                rate >= 60.0 -> "c"
                else -> "d"
            }
        }

        fun difficultyName(index: Int, isUtage: Boolean): String {
            if (isUtage) return "U.TA.GE"
            return when (index) {
                0 -> "BASIC"
                1 -> "ADVANCED"
                2 -> "EXPERT"
                3 -> "MASTER"
                4 -> "Re:MASTER"
                else -> "U.TA.GE"
            }
        }

        fun difficultyColors(index: Int, isUtage: Boolean): List<String> {
            if (isUtage) return ColorArray.magenta
            return when (index) {
                0 -> ColorArray.green
                1 -> ColorArray.yellow
                2 -> ColorArray.red
                3 -> ColorArray.purple
                4 -> ColorArray.sakura
                else -> ColorArray.magenta
            }
        }

        fun maimaiBannerIndex(songID: Int, version: String?): Int {
            val map = mapOf(
                11069 to 2, 834 to 7, 567 to 17, 417 to 28, 10734 to 35,
                734 to 35, 838 to 42, 11591 to 45, 647 to 48, 11662 to 51,
                11663 to 53, 844 to 54, 606 to 56, 270 to 59, 11355 to 62,
                11820 to 64, 11512 to 69, 11629 to 70, 227 to 71,
                11711 to 78, 11026 to 83,
            )
            map[songID]?.let { return it }
            if (version == "maimai でらっくす Splash" || version == "maimai でらっくす Splash PLUS") return 11
            return 0
        }

        fun chunithmRank(score: Int): String = when {
            score >= 1009000 -> "sssp"
            score >= 1007500 -> "sss"
            score >= 1005000 -> "ssp"
            score >= 1000000 -> "ss"
            score >= 975000 -> "s"
            score >= 950000 -> "aaa"
            score >= 925000 -> "aa"
            score >= 900000 -> "a"
            score >= 800000 -> "bbb"
            score >= 700000 -> "bb"
            score >= 600000 -> "b"
            score >= 500000 -> "c"
            else -> "d"
        }

        fun chunithmRankBackgroundFile(score: Int): String {
            val suffix = when (chunithmRank(score)) {
                "sssp" -> "PP"
                "sss" -> "PF"
                "ssp", "ss" -> "SSS"
                "sp", "s" -> "X"
                "aaa", "aa", "a" -> "S"
                "bbb", "bb", "b" -> "B"
                "c" -> "A"
                else -> "F"
            }
            return "object-score-backimage-$suffix.webp"
        }

        fun chunithmRatingBackgroundFile(rating: Double): String {
            val suffix = when {
                rating < 4 -> "A"
                rating < 7 -> "SP"
                rating < 10 -> "D"
                rating < 12 -> "C"
                rating < 13.25 -> "S"
                rating < 14.5 -> "SH"
                rating < 15.25 -> "X"
                rating < 16 -> "XH"
                rating < 17 -> "PF"
                else -> "PP"
            }
            return "object-score-backimage-$suffix.webp"
        }

        fun chunithmDifficultyColor(index: Int): String {
            return listOf("#009944", "#fff100", "#d32f2f", "#9922ee", "#000000")
                .getOrElse(index) { "#009944" }
        }

    }
}
