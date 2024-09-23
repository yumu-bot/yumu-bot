package com.now.nowbot.throwable.serviceException;

import com.now.nowbot.throwable.TipsException;

public class DiceException extends TipsException {
    public enum Type {
        DICE_Instruction("""
                欢迎使用 Yumu Dice 功能！食用方法：
                !ymdice / !d (number) (compare)
                number：要 roll 的数字。不输入默认 100。
                compare：要比较的字符串，最好使用中文常见关联词。也可以输入冒号来分隔需要比较的两个对象。"""),
        DICE_Dice_TooMany("扔了 %s 枚骰子，也排解不了你内心的空虚吗？"),
        DICE_Number_TooLarge("选这么大干什么！"),
        DICE_Number_TooSmall("和你的杂鱼脑子一样小！"),
        DICE_Number_ParseFailed("请不要输入天文数字！"),
        DICE_Number_NotSupportNegative("请不要输入负数！"),
        DICE_Compare_NotMatch("我也不知道该选什么...扔骰子解决吧。"),
        DICE_Compare_All("我全都要！！！"),
        DICE_Compare_NoDifference("有区别？重新选吧！"),
        DICE_Compare_Tie("硬币立在墙角了。再投一次？"),
        DICE_Compare_Wtf("你不可能看到这条消息。"),
        DICE_EasterEgg_0d00("打个角先..."),
        DICE_Send_Error("我真笨，连个骰子都扔不好..."),

        ;//逗号分隔
        public final String message;
        Type(String message) {
            this.message = message;
        }
    }
    public DiceException(DiceException.Type type){
        super(type.message);
    }

    public DiceException(DiceException.Type type, Object... args){
        super(String.format(type.message, args));
    }
}
