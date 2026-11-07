package com.now.nowbot.image.draw

import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.Colors
import com.now.nowbot.image.util.Numbers
import com.now.nowbot.image.util.Texts
import com.now.nowbot.model.osu.LazerMod
import java.util.Locale
import kotlin.math.max

private val stdBonus = mapOf(
    "DC" to 0.3, "HT" to 0.3, "NF" to 0.5, "EZ" to 0.5, "HD" to 1.06, "HR" to 1.06,
    "DT" to 1.1, "NC" to 1.1, "RX" to 0.1, "FL" to 1.12, "BL" to 1.12, "SO" to 0.9,
    "TP" to 0.1, "DA" to 0.5, "CL" to 0.96, "WU" to 0.5, "WD" to 0.5, "MG" to 0.5,
    "AS" to 0.5, "SY" to 0.8,
)
private val taikoBonus = mapOf(
    "DC" to 0.3, "HT" to 0.3, "NF" to 0.5, "EZ" to 0.5, "HD" to 1.06, "HR" to 1.06,
    "DT" to 1.1, "NC" to 1.1, "RX" to 0.1, "FL" to 1.12, "SO" to 0.9, "DA" to 0.5,
    "CL" to 0.96, "CS" to 0.9, "WU" to 0.5, "WD" to 0.5, "AS" to 0.5,
)
private val catchBonus = mapOf(
    "DC" to 0.3, "HT" to 0.3, "NF" to 0.5, "EZ" to 0.5, "HD" to 1.06, "HR" to 1.12,
    "DT" to 1.1, "NC" to 1.1, "RX" to 0.1, "FL" to 1.12, "SO" to 0.9, "DA" to 0.5,
    "CL" to 0.96, "WU" to 0.5, "WD" to 0.5,
)
private val maniaBonus = mapOf(
    "DC" to 0.3, "HT" to 0.3, "NR" to 0.9, "NF" to 0.5, "EZ" to 0.5, "DA" to 0.5,
    "CL" to 0.96, "CS" to 0.9, "HO" to 0.9, "WU" to 0.5, "WD" to 0.5, "AS" to 0.5,
)

data class ModDrawResult(val svg: String, val width: Double)

object Mods {
    fun multiplier(mods: List<LazerMod>, rulesetId: Int): Double {
        val bonus = when (rulesetId) {
            1 -> taikoBonus
            2 -> catchBonus
            3 -> maniaBonus
            else -> stdBonus
        }
        var multiplier = 1.0
        for (mod in mods) multiplier *= bonus[mod.acronym] ?: 1.0
        return multiplier
    }

    fun drawLazerMods(
        mods: List<LazerMod>,
        x: Double = 0.0,
        y: Double = 0.0,
        height: Double = 100.0,
        maxWidth: Double = Double.POSITIVE_INFINITY,
        align: String = "left",
        interval: Double = 8.0,
        allowExpanded: Boolean = true,
        allowName: Boolean = true,
        assets: ImageAssets,
    ): ModDrawResult {
        if (mods.isEmpty()) return ModDrawResult("", 0.0)

        val plan = layoutPlan(mods, height, maxWidth, interval, allowExpanded, allowName)
        val layout = plan.layout
        val sorted = plan.sorted
        val hasExpanded = plan.hasExpanded
        val scale = height / 100.0

        val right = align == "right"
        val left = when {
            right -> x - scale * (if ((layout.expanded && hasExpanded) || layout.name) 235.0 else 135.0)
            align == "center" -> x - layout.total / 2
            else -> x
        }

        val builder = StringBuilder()
        var deltaX = 0.0
        var sideName = layout.sideName
        sorted.forEachIndexed { index, mod ->
            if (index == sorted.lastIndex) sideName = false
            deltaX += when {
                index == 0 -> 0.0
                layout.name || (layout.expanded && mod.settings != null) -> if (right) -layout.hasOffset else layout.hasOffset
                else -> if (right) -layout.noOffset else layout.noOffset
            }
            builder.append(
                modPath(mod, left + deltaX, y, height, layout.expanded, layout.name, sideName, assets),
            )
        }
        return ModDrawResult(builder.toString(), layout.total)
    }

    /**
     * 只计算 [drawLazerMods] 会占用的宽度，不读取字体或图片素材。
     * 与绘制共用同一套布局选择，避免裁切宽度和实际绘制不一致。
     */
    fun lazerModsWidth(
        mods: List<LazerMod>,
        height: Double = 100.0,
        maxWidth: Double = Double.POSITIVE_INFINITY,
        interval: Double = 8.0,
        allowExpanded: Boolean = true,
        allowName: Boolean = true,
    ): Double {
        if (mods.isEmpty()) return 0.0
        return layoutPlan(mods, height, maxWidth, interval, allowExpanded, allowName).layout.total
    }

    private fun layoutPlan(
        mods: List<LazerMod>,
        height: Double,
        maxWidth: Double,
        interval: Double,
        allowExpanded: Boolean,
        allowName: Boolean,
    ): ModLayoutPlan {
        val scale = height / 100.0
        val withSettings = mods.filter { it.settings != null }
        val withoutSettings = mods.filter { it.settings == null }
        val sorted = withSettings + withoutSettings

        val fullWidth = modWidth(sorted.size, 235.0, scale, interval)
        val standardWidth = modWidth(withSettings.size, 235.0, scale, interval) +
            modWidth(withoutSettings.size, 135.0, scale, interval) +
            modInterval(withSettings, withoutSettings, scale, interval)
        val halfInterval = interval - 135.0 * scale / 2
        val halfWidth = modWidth(withSettings.size, 235.0, scale, interval) +
            modWidth(withoutSettings.size, 135.0, scale, halfInterval) +
            modInterval(withSettings, withoutSettings, scale, 235.0 * scale - halfInterval)
        val minimumInterval = interval - 135.0 * scale * 3 / 4
        val minimumWidth = modWidth(withSettings.size, 235.0, scale, interval) +
            modWidth(withoutSettings.size, 135.0, scale, minimumInterval) +
            modInterval(withSettings, withoutSettings, scale, 235.0 * scale - minimumInterval)

        val hasExpanded = withSettings.isNotEmpty()
        val layout = when {
            fullWidth <= maxWidth && allowName -> ModLayout(
                fullWidth, allowExpanded, true, false,
                interval + 235.0 * scale, interval + 235.0 * scale,
            )
            standardWidth <= maxWidth -> ModLayout(
                standardWidth, allowExpanded, false, false,
                interval + 135.0 * scale, interval + 235.0 * scale,
            )
            halfWidth <= maxWidth -> ModLayout(
                halfWidth, allowExpanded, false, false,
                interval + 135.0 / 2 * scale, interval + 235.0 * scale,
            )
            minimumWidth <= maxWidth && hasExpanded -> ModLayout(
                minimumWidth, allowExpanded, false, true,
                interval + 135.0 * 3.0 / 4.0 * scale, interval + 235.0 * scale,
            )
            else -> {
                val offset = max((maxWidth + interval - 135.0 * scale) / sorted.size, 4.0)
                ModLayout(maxWidth, false, false, true, offset, offset)
            }
        }
        return ModLayoutPlan(layout, sorted, hasExpanded)
    }

    private data class ModLayoutPlan(
        val layout: ModLayout,
        val sorted: List<LazerMod>,
        val hasExpanded: Boolean,
    )

    private fun modWidth(count: Int, width: Double, scale: Double, interval: Double): Double {
        return max(count * (width * scale + interval) - interval, 0.0)
    }

    private fun modInterval(first: List<LazerMod>, second: List<LazerMod>, scale: Double, interval: Double): Double {
        return if (first.isNotEmpty() && second.isNotEmpty()) scale * interval else 0.0
    }

    private fun modPath(
        mod: LazerMod,
        x: Double,
        y: Double,
        height: Double,
        allowExpanded: Boolean,
        allowName: Boolean,
        allowSideName: Boolean,
        assets: ImageAssets,
    ): String {
        val scale = height / 100.0
        val modName = mod.acronym.ifBlank { "NM" }
        val modColor = mod.color.ifBlank { Colors.getModColor(modName) }
        val index = Texts.random(6)
        val isDark = Colors.hex2OKLabBrightness(modColor) <= 0.5
        val iconColor = if (isDark) {
            Colors.changeHexOKLabLightnessProgressive(modColor, 0.4)
        } else {
            Colors.changeHexOKLabLightnessProgressive(modColor, -0.4)
        }

        val hasSettings = mod.settings != null
        val defSettings = if (hasSettings) {
            maskImage("mask-M-Settings-$index", 91.0 * scale, 2.0 * scale, 40.0 * scale, 28.0 * scale, assets.local("Mods/at.png"))
        } else {
            ""
        }
        val defIcon = maskImage(
            "mask-M-Icon-$index", 7.5 * scale, 10.0 * scale, 120.0 * scale, 84.0 * scale,
            assets.local("Mods/${modName.lowercase(Locale.ROOT)}.png"),
        )
        val defBase = maskImage("mask-M-Base-$index", 0.0, 3.0 * scale, 135.0 * scale, 97.0 * scale, assets.local("Mods/mod-icon.png"))

        var showExtender = true
        var nameColor = modColor
        val defName: String
        if (allowExpanded && hasSettings) {
            val additional = additional(mod)
            defName = maskPath(
                "mask-M-Name-$index",
                Fonts.torusBold.get2SizeTextPath(
                    additional.large, additional.small,
                    48.0 * scale, 30.0 * scale, 176.0 * scale, 66.0 * scale, "center baseline", "#fff",
                ),
            )
            nameColor = if (isDark) {
                Colors.changeHexOKLabLightnessProgressive(modColor, -0.2)
            } else {
                Colors.changeHexOKLabLightnessProgressive(modColor, 0.2)
            }
        } else if (allowName) {
            defName = maskPath(
                "mask-M-Name-$index",
                Fonts.torusBold.getTextPath(modName, 176.0 * scale, 66.0 * scale, 48.0 * scale, "center baseline", "#fff"),
            )
        } else {
            defName = ""
            showExtender = false
        }

        val defExtender = if (showExtender) {
            maskImage("mask-M-Extender-$index", 100.0 * scale, 17.0 * scale, 135.0 * scale, 68.0 * scale, assets.local("Mods/mod-icon-extender.png"))
        } else {
            ""
        }

        val colorSettings = PanelDraw.rect(91.0 * scale, 2.0 * scale, 40.0 * scale, 28.0 * scale, 0.0, modColor, 1.0)
        val colorIcon = PanelDraw.rect(7.5 * scale, 8.0 * scale, 120.0 * scale, 84.0 * scale, 0.0, iconColor, 1.0)
        val colorBase = PanelDraw.rect(0.0, 3.0 * scale, 135.0 * scale, 97.0 * scale, 0.0, modColor, 1.0)
        val colorName = if (showExtender) PanelDraw.rect(130.0 * scale, 0.0, 105.0 * scale, 100.0 * scale, 0.0, nameColor, 1.0) else ""
        val colorExtender = if (showExtender) PanelDraw.rect(100.0 * scale, 17.0 * scale, 135.0 * scale, 68.0 * scale, 0.0, iconColor, 1.0) else ""
        val settingsBase = if (hasSettings) PanelDraw.circle((95.0 + 16.0) * scale, 16.0 * scale, 16.0 * scale, iconColor, 1.0) else ""
        val sideName = if (!showExtender && allowSideName) {
            "<g transform=\"rotate(-60 ${Svg.number(92 * scale)} ${Svg.number(72 * scale)})\">" +
                Fonts.torusBold.getTextPath(mod.acronym, 92 * scale, 72 * scale, 20 * scale, "left top", iconColor) + "</g>"
        } else {
            ""
        }

        val body = buildString {
            append("<defs>")
            append(defSettings)
            append(defIcon)
            append(defBase)
            append(defName)
            append(defExtender)
            append("</defs>")
            append("<g id=\"Extender_M_$index\" mask=\"url(#mask-M-Extender-$index)\">")
            append(if (showExtender) colorExtender else "")
            append("</g>")
            append("<g id=\"Name_M_$index\" mask=\"url(#mask-M-Name-$index)\">")
            append(if (showExtender) colorName else "")
            append("</g>")
            append("<g id=\"Base_M_$index\" mask=\"url(#mask-M-Base-$index)\">")
            append(colorBase)
            append("</g>")
            append("<g id=\"Icon_M_$index\" mask=\"url(#mask-M-Icon-$index)\">")
            append(colorIcon)
            append("</g>")
            append("<g id=\"Settings_Base_M_$index\">")
            append(settingsBase)
            append("</g>")
            append("<g id=\"Side_Name_M_$index\">")
            append(sideName)
            append("</g>")
            append("<g id=\"Settings_M_$index\" mask=\"url(#mask-M-Settings-$index)\">")
            append(if (hasSettings) colorSettings else "")
            append("</g>")
        }
        return "<g id=\"Mod_$index\" transform=\"translate(${Svg.number(x)} ${Svg.number(y)})\">$body</g>"
    }

    private fun maskImage(mask: String, x: Double, y: Double, width: Double, height: Double, href: String?): String {
        if (href.isNullOrBlank()) return ""
        return "<mask id=\"${Svg.escape(mask)}\" xmlns:xlink=\"http://www.w3.org/1999/xlink\">" +
            "<image x=\"${Svg.number(x)}\" y=\"${Svg.number(y)}\" width=\"${Svg.number(width)}\" " +
            "height=\"${Svg.number(height)}\" preserveAspectRatio=\"xMidYMid meet\" " +
            "xlink:href=\"${Svg.escape(href)}\"/></mask>"
    }

    private fun maskPath(mask: String, path: String): String {
        return "<mask id=\"${Svg.escape(mask)}\">$path</mask>"
    }

    private data class ModAdditional(val large: String, val small: String)

    private fun additional(mod: LazerMod): ModAdditional {
        val settings = mod.settings
        if (settings == null) return ModAdditional(mod.acronym, "")

        val acronym = mod.acronym
        fun number(key: String): Double? {
            val value = setting(settings, key)
            return if (Numbers.isNumber(value)) Numbers.toDouble(value) else null
        }
        fun isTrue(key: String): Boolean = setting(settings, key) == true

        return when {
            matches(acronym, "DT", "NC", "HT", "DC") && number("speed_change") != null -> {
                val value = Numbers.roundParts(number("speed_change"), 2, -2)
                ModAdditional(value.integer, value.decimal + "x")
            }
            matches(acronym, "WU", "WD") && number("final_rate") != null -> {
                val value = Numbers.roundParts(number("final_rate"), 2, -2)
                ModAdditional(value.integer, value.decimal + "x")
            }
            matches(acronym, "EZ") && number("extra_lives") != null -> {
                ModAdditional(displayText(setting(settings, "extra_lives")), "UP")
            }
            matches(acronym, "AS") && number("initial_rate") != null -> {
                val value = Numbers.roundParts(number("initial_rate"), 2, -2)
                ModAdditional(value.integer, value.decimal + "x")
            }
            matches(acronym, "AC") -> {
                val accuracy = number("minimum_accuracy")?.let { Numbers.jsRound(it * 100).toLong().toString() } ?: "90"
                ModAdditional(accuracy, if (setting(settings, "accuracy_judge_mode") == "1") "+" else "%")
            }
            matches(acronym, "SR") -> when {
                setting(settings, "one_sixth_conversion") == false -> when {
                    setting(settings, "one_eighth_conversion") == true -> ModAdditional("-1", "/8")
                    setting(settings, "one_third_conversion") == true -> ModAdditional("-1", "/3")
                    else -> ModAdditional("", "")
                }
                else -> ModAdditional("-1", "/6")
            }
            matches(acronym, "DA") -> when {
                isTrue("extended_limits") -> ModAdditional("DA+", "")
                number("scroll_speed") != null -> {
                    val value = Numbers.floorParts(number("scroll_speed"), 1, -2)
                    ModAdditional("v" + value.integer, value.decimal + "x")
                }
                isTrue("hard_rock_offsets") -> ModAdditional("HR", "")
                else -> ModAdditional("DA", "")
            }
            else -> ModAdditional(acronym, "")
        }
    }

    private fun setting(settings: Any, key: String): Any? {
        if (settings is Map<*, *>) {
            settings[key]?.let { return it }
            return settings[snakeToCamel(key)]
        }
        val camel = snakeToCamel(key)
        val type = settings.javaClass
        val field = type.declaredFields.firstOrNull { it.name == camel || it.name == key }
        if (field != null) {
            return runCatching {
                field.isAccessible = true
                field.get(settings)
            }.getOrNull()
        }
        val getterName = "get" + camel.replaceFirstChar { it.uppercase() }
        val getter = type.methods.firstOrNull { it.name.equals(getterName, ignoreCase = true) }
        if (getter != null) {
            return runCatching { getter.invoke(settings) }.getOrNull()
        }
        return null
    }

    private fun snakeToCamel(value: String): String {
        val parts = value.split('_')
        return parts.first() + parts.drop(1).joinToString("") { it.replaceFirstChar { c -> c.uppercase() } }
    }

    private fun matches(acronym: String, vararg names: String): Boolean {
        return names.any { it.equals(acronym, ignoreCase = true) }
    }

    private fun displayText(value: Any?): String {
        return when (value) {
            null -> ""
            is Double -> if (value.isFinite() && value == kotlin.math.floor(value)) value.toLong().toString() else value.toString()
            is Float -> {
                val number = value.toString().toDouble()
                if (number.isFinite() && number == kotlin.math.floor(number)) number.toLong().toString() else value.toString()
            }
            else -> value.toString()
        }
    }

    private data class ModLayout(
        val total: Double,
        val expanded: Boolean,
        val name: Boolean,
        val sideName: Boolean,
        val noOffset: Double,
        val hasOffset: Double,
    )
}
