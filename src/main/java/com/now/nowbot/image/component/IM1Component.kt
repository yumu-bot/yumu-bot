package com.now.nowbot.image.component

import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.PanelDraw
import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.model.osu.OsuUser

data class IM1Data(
    val background: String? = null,
    val avatar: String? = null,
    val name: String = "Who",
    val groups: List<OsuUser.UserGroup> = emptyList(),
) {
    companion object {
        fun fromUser(user: OsuUser?, assets: ImageAssets): IM1Data {
            val background = assets.remote(user?.coverUrl)
                ?: assets.remote(user?.cover?.url)
                ?: assets.localBanner()
            val avatar = assets.remote(user?.avatarUrl) ?: assets.local("avatar-guest.png")
            val name = user?.username?.takeIf { it.isNotBlank() }
                ?: user?.let { "ID: ${it.userID}" }
                ?: "Unknown"
            return IM1Data(background, avatar, name, user?.groups.orEmpty())
        }
    }
}

object IM1Component {
    private const val MARK_TEXT = "<g id=\"Text_CO1\">"
    private const val MARK_LABEL = "<g id=\"Label_CO1\">"
    private const val MARK_BACKGROUND = "<g style=\"clip-path: url(#clippath-CO1-1);\">"
    private const val MARK_AVATAR = "<g style=\"clip-path: url(#clippath-CO1-2);\">"

    private val DEFAULT_GROUP = OsuUser.UserGroup(color = "#382E32", id = -1, identifier = "mapper", name = "Normal Mapper", shortName = "M")

    fun render(data: IM1Data, assets: ImageAssets): String {
        var svg = """
   <defs>
            <clipPath id="clippath-CO1-1">
              <rect width="390" height="210" rx="20" ry="20" style="fill: none;"/>
            </clipPath>
            <clipPath id="clippath-CO1-2">
              <rect x="145" y="15" width="100" height="100" rx="10" ry="10" style="fill: none;"/>
            </clipPath>
          </defs>
          <g id="Background_CO1">
            <rect width="100" height="100" rx="20" ry="20" style="fill: #382E32;"/>
            <g style="clip-path: url(#clippath-CO1-1);">
            </g>
          </g>
          <g id="Label_CO1">
          </g>
          <g id="Avatar_CO1">
            <g style="clip-path: url(#clippath-CO1-2);">
            </g>
          </g>
          <g id="Text_CO1">
          </g>
        """.trimIndent()

        val title = Fonts.torus.textPath(data.name, 195.0, 155.0, 36.0, "center baseline", "#fff")
        svg = Svg.setText(svg, title, MARK_TEXT)

        val groups = if (data.groups.isNotEmpty()) data.groups else listOf(DEFAULT_GROUP)
        val groupCount = minOf(groups.size, 4)
        for (index in 0 until groupCount) {
            val group = groups[index]
            val width = groupCount * 90.0 - 10
            val startX = (390 - width) / 2
            val x = startX + index * 90.0
            svg = Svg.setText(svg, Svg.group(x, 170.0, groupLabel(group)), MARK_LABEL)
        }

        svg = Svg.setText(
            svg,
            data.avatar?.let { Svg.image(145.0, 15.0, 100.0, 100.0, it) },
            MARK_AVATAR,
        )
        svg = Svg.setText(
            svg,
            data.background?.let { Svg.image(0.0, 0.0, 390.0, 210.0, it, 0.6) },
            MARK_BACKGROUND,
        )
        return svg
    }

    private fun groupLabel(group: OsuUser.UserGroup): String {
        val color = group.color?.takeIf { it.isNotBlank() } ?: "#382E32"
        return PanelDraw.rect(0.0, 0.0, 80.0, 28.0, 14.0, color) +
            Fonts.torus.textPath(group.shortName, 40.0, 22.0, 24.0, "center baseline", "#fff")
    }
}
