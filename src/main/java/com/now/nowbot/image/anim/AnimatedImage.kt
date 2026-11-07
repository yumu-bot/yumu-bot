package com.now.nowbot.image.anim

import java.awt.image.BufferedImage

data class AnimatedImage(
    val width: Int,
    val height: Int,
    val frames: List<BufferedImage>,
    val delaysMs: List<Int>,
    val loopCount: Int,
    val hasAlpha: Boolean,
)
