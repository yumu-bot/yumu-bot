package com.now.nowbot.image.util

object PanelColor {
    fun base(hue: Int = 342): String = hsl2hex(hueOr(hue).toDouble(), 0.1, 0.1)

    fun bottom(hue: Int = 342): String = hsl2hex(hueOr(hue).toDouble(), 0.1, 0.15)

    fun middle(hue: Int = 342): String = hsl2hex(hueOr(hue).toDouble(), 0.1, 0.2)

    fun top(hue: Int = 342): String = hsl2hex(hueOr(hue).toDouble(), 0.1, 0.25)

    fun overlay(hue: Int = 342): String = hsl2hex(hueOr(hue).toDouble(), 0.1, 0.3)

    fun bright(hue: Int = 342): String = hsl2hex(hueOr(hue).toDouble(), 0.1, 0.7)

    private fun hueOr(hue: Int): Int = if (hue == 0) 342 else hue

    fun hsl2hex(hue: Double = 0.0, saturation: Double = 0.0, lightness: Double = 0.0): String {
        var normalizedHue = if (hue > 1) hue / 360 else hue

        val red: Double
        val green: Double
        val blue: Double
        if (saturation == 0.0) {
            red = lightness * 255.0
            green = lightness * 255.0
            blue = lightness * 255.0
        } else {
            val var2 = if (lightness < 0.5) {
                lightness * (1 + saturation)
            } else {
                (lightness + saturation) - (saturation * lightness)
            }
            val var1 = 2.0 * lightness - var2

            red = 255.0 * hue2rgb(var1, var2, normalizedHue + 1.0 / 3.0)
            green = 255.0 * hue2rgb(var1, var2, normalizedHue)
            blue = 255.0 * hue2rgb(var1, var2, normalizedHue - 1.0 / 3.0)
        }

        return "#" + channel(red) + channel(green) + channel(blue)
    }

    private fun channel(value: Double): String {
        return Numbers.jsRound(value).toLong().toString(16).padStart(2, '0')
    }

    private fun hue2rgb(v1: Double, v2: Double, raw: Double): Double {
        var vh = raw
        if (vh < 0) vh += 1
        if (vh > 1) vh -= 1
        if (6.0 * vh < 1) return v1 + (v2 - v1) * 6.0 * vh
        if (2.0 * vh < 1) return v2
        if (3.0 * vh < 2) return v1 + (v2 - v1) * ((2.0 / 3.0) - vh) * 6.0
        return v1
    }
}
