package com.now.nowbot.model.enums

enum class MaiUtage(val alias: String) {
    // 所有音符都是 Break。
    LIGHT("光"),

    // 所有音符都是 Slide 头。
    STAR("星"),

    // 所有音符的 Slide 头会跑到尾巴去。
    REVERSE("回"),

    // 有隐藏音符，需要记忆
    MEMORISE("覚"),

    // slide 没有星星头
    STROKE("撫"),

    // slide 不需要等一拍
    INSTANT("即"),

    // 谱面是倾斜的
    TILT("倾"),

    // 有三押四押，需要玩家用手肘
    OCTOPUS("蛸"),

    // 推荐多人游玩同一台机器
    COLLABORATION("協"),

    // 耐力谱
    ENDURE("耐"),

    // 不属于任何分类的谱面
    BANQUET("宴"),

    // 本来打算做魔王曲的谱面，但是太难被移到这个分类里
    WRAPPED("蔵"),

    // 比以上两个更难
    INSANE("狂"),
}
