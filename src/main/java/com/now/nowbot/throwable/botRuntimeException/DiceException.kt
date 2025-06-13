package com.now.nowbot.throwable.botRuntimeException

import com.now.nowbot.throwable.TipsRuntimeException

open class DiceException(message: String?): TipsRuntimeException(message) {
    class DiceTooMany(count: Number):
        DiceException("扔了 $count 枚骰子，也排解不了你内心的空虚吗？")

    class Exceed:
        DiceException("请不要输入天文数字！")

    class Negative:
        DiceException("请不要输入负数！")

    class NoDifference:
        DiceException("有区别吗？重新选吧！")

    class NotMatched:
        DiceException("我也不知道该选什么...简单扔个骰子解决吧。")

    class TooLarge:
        DiceException("选这么大干什么！")

    class TooSmall:
        DiceException("和你的杂鱼脑子一样小！")

    class Unexpected:
        DiceException("我真笨，连个骰子都扔不好...")

    // 以下是彩蛋部分
    class All:
        DiceException("我全都要！！！")

    class ForWhat:
        DiceException("干什么¿")

    class NoDifferenceEveryday(left: String, right: String):
        DiceException("""
            这么喜欢 $left？
            你这辈子就天天研究 $right 去吧！
            """.trimIndent())

    class JerkOff:
        DiceException("打个角先...")

    class Tie:
        DiceException("硬币立在墙角了。再投一次？")
}