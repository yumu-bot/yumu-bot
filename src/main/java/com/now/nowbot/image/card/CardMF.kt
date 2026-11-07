package com.now.nowbot.image.card

import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.maimai.MaiAssets
import com.now.nowbot.image.maimai.MaiDraw
import com.now.nowbot.image.maimai.MaiLabel
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.Texts
import com.now.nowbot.model.maimai.MaiSong

object CardMF {
    private val difficultyColors = listOf(
        "#7CC576", "#FFF568", "#F26D7D", "#A864A8", "#FAD7F7",
    )

    fun render(song: MaiSong, assets: ImageAssets): String {
        val mai = MaiAssets(assets)

        val titleText = song.info.title
        val hasAlias = !song.alias.isNullOrBlank()
        val left1Text = if (hasAlias) song.alias.orEmpty() else song.info.artist
        val left2Text = if (hasAlias) song.info.artist else ""

        val titleFont = if (Texts.isAscii(titleText)) Fonts.torus else Fonts.puHuiTi
        val left1Font = if (Texts.isAscii(left1Text)) Fonts.torus else Fonts.puHuiTi
        val left2Font = if (Texts.isAscii(left2Text)) Fonts.torus else Fonts.puHuiTi
        val titleSize = if (Texts.isAscii(titleText)) 26.0 else 24.0
        val left1Size = if (Texts.isAscii(left1Text)) 20.0 else 18.0
        val left2Size = if (Texts.isAscii(left2Text)) 20.0 else 18.0

        val title = titleFont.textPath(
            titleFont.cutStringTail(titleText, titleSize, 255.0),
            187.0, 34.0, titleSize, "left baseline", "#fff",
        )
        val left1 = left1Font.textPath(
            left1Font.cutStringTail(left1Text, left1Size, 255.0),
            187.0, 64.0, left1Size, "left baseline", "#bbb",
        )
        val left2 = left2Font.textPath(
            left2Font.cutStringTail(left2Text, left2Size, 255.0),
            187.0, 90.0, left2Size, "left baseline", "#bbb",
        )

        val difficulty = drawDifficultyLabels(song)
        val cover = mai.cover(song.songID.toLong())
        val genre = MaiDraw.labelA8(
            MaiAssets.category(song.info.genre),
            mai.favoriteIcon(),
            true,
            "#1c1719",
            imageSlot = true,
        )
        val bpm = MaiDraw.labelA8(
            song.info.bpm.toString(),
            mai.playcountIcon(),
            true,
            "#1c1719",
            imageSlot = true,
        )
        val sid = MaiDraw.labelA8(song.songID, null, null, "#1c1719")

        val baseColor = song.info.versionColor.takeIf { it.isNotBlank() } ?: "none"
        val base = Svg.rect(0.0, 0.0, 445.0, 210.0, 20.0, baseColor, 0.3) +
            Svg.rect(0.0, 0.0, 445.0, 175.0, 20.0, "#382E32", 1.0)

        val type = mai.typeImage(song.type)
        val version = mai.versionBackground(song.info.version)

        return buildString {
            append(
                """
                <defs>
                    <clipPath id="clippath-CMF-1">
                      <rect x="135" width="310" height="175" rx="20" ry="20" style="fill: none;"/>
                    </clipPath>
                    <clipPath id="clippath-CMF-2">
                      <rect width="175" height="175" rx="20" ry="20" style="fill: none;"/>
                    </clipPath>
                    <filter id="blur-CMF-1" height="120%" width="120%" x="-10%" y="-10%" filterUnits="userSpaceOnUse">
                        <feGaussianBlur in="userSpaceOnUse" stdDeviation="5" result="blur"/>
                    </filter>
                </defs>
                """.trimIndent()
            )
            append("<g id=\"Base_CMF\">")
            append(base)
            append("</g>")
            append("<g id=\"Background_CMF\"><g style=\"clip-path: url(#clippath-CMF-1);\" filter=\"url(#blur-CMF-1)\">")
            append(Svg.image(135.0, 0.0, 310.0, 175.0, cover.orEmpty(), 0.3))
            append("</g></g>")
            append("<g id=\"Cover_CMF\"><g style=\"clip-path: url(#clippath-CMF-2);\">")
            append(Svg.image(0.0, 0.0, 175.0, 175.0, cover.orEmpty(), 1.0))
            append("</g></g>")
            append("<g id=\"Text_CMF\">")
            append(title)
            append(left1)
            append(left2)
            append("</g>")
            append("<g id=\"Label_CMF\">")
            append(Svg.group(15.0, 175.0, difficulty))
            append(Svg.group(183.0, 114.0, genre.svg))
            append(Svg.group(183.0, 144.0, bpm.svg))
            append(Svg.group(445.0 - sid.width - 10.0, 144.0, sid.svg))
            append(Svg.image(5.0, 5.0, 60.0, 40.0, type.orEmpty(), 1.0))
            append(Svg.image(80.0, 128.0, 90.0, 45.0, version.orEmpty(), 1.0))
            append("</g>")
        }
    }

    private fun drawDifficultyLabels(song: MaiSong): String {
        val labels = mutableListOf<MaiLabel>()

        if (song.star.size < 4) {
            labels += MaiDraw.labelA8(
                song.level.getOrNull(0).orEmpty(),
                null,
                true,
                "#F06EA9",
            )
        } else {
            song.star.forEachIndexed { index, ds ->
                val highlight = song.highlight
                val isHighlight = when {
                    highlight == null -> true
                    highlight.isEmpty() -> true
                    else -> index in highlight
                }
                labels += MaiDraw.labelA8(
                    Svg.number(ds),
                    null,
                    true,
                    difficultyColors.getOrElse(index) { difficultyColors.last() },
                    isHighlight,
                )
            }
        }

        var x = 0.0
        return buildString {
            labels.forEach { label ->
                append(Svg.group(x, 5.0, label.svg))
                x += label.width + 8.0
            }
        }
    }
}
