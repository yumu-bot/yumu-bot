package com.now.nowbot.image.component.profile

import com.now.nowbot.image.draw.PanelDraw
import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.util.Numbers
import kotlin.math.max

data class D7Data(
    val mode: String = "osu",
    val country: String = "CN",
    val pp4k: Double = 0.0,
    val pp7k: Double = 0.0,
    val global4k: Long = 0,
    val global7k: Long = 0,
    val country4k: Long = 0,
    val country7k: Long = 0,
)

object D7Component {
    fun render(data: D7Data): String {
        if (data.mode != "mania") return ""
        val key4 = "4K: ${Numbers.jsRound(data.pp4k).toLong()}PP // #${data.global4k} ${data.country}#${data.country4k}"
        val key7 = "7K: ${Numbers.jsRound(data.pp7k).toLong()}PP // #${data.global7k} ${data.country}#${data.country7k}"
        val width = max(Fonts.torus.width(key4, 24.0), Fonts.torus.width(key7, 24.0))
        return PanelDraw.rect(1840.0 - width, 200.0, width + 40.0, 70.0, 20.0, "#382E32") +
            Fonts.torus.textPath(key4, 1860.0 - width, 228.0, 24.0, "left baseline", "#fff") +
            Fonts.torus.textPath(key7, 1860.0 - width, 258.0, 24.0, "left baseline", "#fff")
    }
}
