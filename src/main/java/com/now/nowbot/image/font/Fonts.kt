package com.now.nowbot.image.font

class FontFace internal constructor(
    val key: String,
    val family: String,
    private val resource: String,
) {
    internal val source: OpenTypeFace by lazy { OpenTypeFace(resource) }

    fun width(text: Any?, size: Double): Double = Typography.width(this, text?.toString().orEmpty(), size)

    fun getTextWidth(text: Any?, size: Double): Double = width(text, size)

    fun textPath(
        text: Any?,
        x: Double,
        y: Double,
        size: Double,
        anchor: String = "left baseline",
        fill: String = "#fff",
        opacity: Double = 1.0,
        shadow: Boolean = false,
        stroke: String = "none",
        strokeWidth: Double = 0.0,
        strokeOpacity: Double = 1.0,
    ): String {
        return Typography.textPath(
            this,
            text?.toString().orEmpty(),
            x,
            y,
            size,
            anchor,
            fill,
            opacity,
            shadow,
            stroke,
            strokeWidth,
            strokeOpacity,
        )
    }

    fun getTextPath(
        text: Any?,
        x: Double,
        y: Double,
        size: Double,
        anchor: String = "left baseline",
        fill: String = "#fff",
        opacity: Double = 1.0,
        shadow: Boolean = false,
        stroke: String = "none",
        strokeWidth: Double = 0.0,
        strokeOpacity: Double = 1.0,
    ): String = textPath(text, x, y, size, anchor, fill, opacity, shadow, stroke, strokeWidth, strokeOpacity)

    fun get2SizeTextPath(
        largerText: Any?,
        smallerText: Any?,
        largeSize: Double,
        smallSize: Double,
        x: Double,
        y: Double,
        anchor: String = "center baseline",
        color: String = "#fff",
    ): String {
        return Typography.twoSizeTextPath(this, largerText, smallerText, largeSize, smallSize, x, y, anchor, color)
    }

    fun cutStringTail(text: Any?, size: Double, maxWidth: Double, ellipsis: Boolean = true): String {
        return Typography.cutStringTail(this, text?.toString().orEmpty(), size, maxWidth, ellipsis)
    }

    fun getTextMetrics(text: Any?, size: Double): TextMetrics = Typography.metrics(this, text?.toString().orEmpty(), size)
}

object Fonts {
    private val faces = linkedMapOf(
        "torus" to FontFace("torus", "Torus", "/static/font/draw/Torus-SemiBold.ttf"),
        "torusBold" to FontFace("torusBold", "Torus Bold", "/static/font/draw/Torus-Bold.ttf"),
        "torusRegular" to FontFace("torusRegular", "Torus Regular", "/static/font/draw/Torus-Regular.ttf"),
        "poppinsBold" to FontFace("poppinsBold", "Poppins Bold", "/static/font/draw/Poppins-Bold.ttf"),
        "puhuiti" to FontFace("puhuiti", "PuHuiTi", "/static/font/draw/AlibabaPuHuiTi3.0-75SemiBold-CJKTGv4.3.ttf"),
        "extra" to FontFace("extra", "Extra", "/static/font/draw/extra.gamemode.ttf"),
        "lineseedsans" to FontFace("lineseedsans", "LineSeedSansBold", "/static/font/draw/LINESeedSans_Bd.ttf"),
        "tahomaregular" to FontFace("tahomaregular", "TahomaRegular", "/static/font/draw/ft71.ttf"),
        "tahomabold" to FontFace("tahomabold", "TahomaBold", "/static/font/draw/tahomabd.ttf"),
        "berlinbold" to FontFace("berlinbold", "BerlinBold", "/static/font/draw/BerlinsansExpert-Bold.ttf"),
    )

    val torus: FontFace get() = face("torus")
    val torusBold: FontFace get() = face("torusBold")
    val torusRegular: FontFace get() = face("torusRegular")
    val poppinsBold: FontFace get() = face("poppinsBold")
    val puHuiTi: FontFace get() = face("puhuiti")
    val extra: FontFace get() = face("extra")
    val lineSeedSans: FontFace get() = face("lineseedsans")
    val tahomaRegular: FontFace get() = face("tahomaregular")
    val tahomaBold: FontFace get() = face("tahomabold")
    val berlinBold: FontFace get() = face("berlinbold")

    fun face(name: String?): FontFace {
        if (name.isNullOrBlank()) return faces.getValue("puhuiti")
        return faces[name.lowercase()] ?: faces.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value
            ?: faces.getValue("puhuiti")
    }
}
