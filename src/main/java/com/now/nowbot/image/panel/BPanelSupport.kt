package com.now.nowbot.image.panel

import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.util.TimeFormat

internal fun bPanelName(
    name: String,
    sign: String,
    requestTime: String = "request time: ${TimeFormat.nowTimestamp()}",
): String {
    val powered = Fonts.torus.textPath(
        "powered by Yumubot v0.8.5 VS // $name", 20.0, 26.84, 24.0, "left baseline", "#fff",
    )
    val request = Fonts.torus.textPath(requestTime, 1900.0, 26.84, 24.0, "right baseline", "#fff")
    val signPath = Fonts.torus.textPath(sign, 607.5, 83.67, 48.0, "center baseline", "#fff")
    return powered + "\n" + request + "\n" + signPath
}
