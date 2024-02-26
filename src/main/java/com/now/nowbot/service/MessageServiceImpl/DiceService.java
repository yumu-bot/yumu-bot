package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.qq.message.MessageReceipt;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.throwable.ServiceException.DiceException;
import com.now.nowbot.util.Instructions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static com.now.nowbot.service.MessageServiceImpl.DiceService.Split.*;

@Service("DICE")
public class DiceService implements MessageService<DiceService.DiceParam> {
    private static final Logger log = LoggerFactory.getLogger(DiceService.class);

    public record DiceParam(Long number, String text) {}

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<DiceParam> data) throws Throwable {
        var m2 = Instructions.DEPRECATED_AYACHI_NENE.matcher(messageText);
        if (m2.find()) throw new DiceException(DiceException.Type.DICE_EasterEgg_0d00);

        var m = Instructions.DICE.matcher(messageText);
        if (!m.find()) return false;

        var number = m.group("number");
        var text = m.group("text");

        if (StringUtils.hasText(text)) {
            if (StringUtils.hasText(number)) {
                data.setValue(new DiceParam(null, (number + text).trim()));
                return true;
            } else if (text.trim().equalsIgnoreCase("help") || text.trim().equalsIgnoreCase("帮助")) {
                throw new DiceException(DiceException.Type.DICE_Instruction);
            } else {
                data.setValue(new DiceParam(null, text.trim()));
                return true;
            }
        }

        if (StringUtils.hasText(number)) {
            try {
                if (number.contains("-")) {
                    throw new DiceException(DiceException.Type.DICE_Number_NotSupportNegative);
                }
                data.setValue(new DiceParam(Long.parseLong(number), null));
            } catch (NumberFormatException e) {
                throw new DiceException(DiceException.Type.DICE_Number_ParseFailed);
            }
        } else {
            data.setValue(new DiceParam(100L, null));
        }
        return true;

        //throw new DiceException(DiceException.Type.DICE_Instruction);
    }

    @Override
    public void HandleMessage(MessageEvent event, DiceParam param) throws Throwable {
        var from = event.getSubject();
        MessageReceipt receipt;

        try {
            if (Objects.nonNull(param.number)) {
                if (param.number >= Integer.MAX_VALUE) {
                    throw new DiceException(DiceException.Type.DICE_Number_TooLarge);
                }
                if (param.number < 1D) {
                    throw new DiceException(DiceException.Type.DICE_Number_TooSmall);
                }

                float r = getRandom(param.number);
                String format = (r <= 1f) ? "%.2f" : "%.0f";

                receipt = from.sendMessage(String.format(format, r));

                //容易被识别成 QQ
                if (r >= 1000_000f && r < 1000_000_000f) {
                    receipt.recallIn(60 * 1000);
                }
                return;
            }

            if (Objects.nonNull(param.text)) {
                var message = Compare(param.text);

                //用于匹配是否被和谐
                var h = Pattern.compile("○|(\\(和谐\\))");
                if (h.matcher(message).find()) {
                    //被和谐就撤回
                    receipt = from.sendMessage(message);
                    receipt.recallIn(60 * 1000);
                } else {
                    from.sendMessage(message);
                }
            }

        } catch (DiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("扔骰子：处理失败", e);
            throw new DiceException(DiceException.Type.DICE_Send_Error);
        }
    }

    /**
     * 超级复杂™的语言学选择器
     * @param s 输入含有比较关系的文本
     * @return 返回随机一个子项
     * @throws DiceException 错
     */
    public String Compare(String s) throws DiceException {
        float result = getRandom(0);
        float boundary;
        String left = "";
        String right = "";
        float num = 0f;
        String is = "";
        String not = "";
        String leftFormat;
        String rightFormat;
        Split split = null;

        final List<Split> splits = Arrays.asList(RANGE, POSSIBILITY, WHETHER, BETTER, COMPARE, OR, AM, WHAT, IS, JUXTAPOSITION, PREFER, HESITATE, EVEN, ASSUME, CONDITION, LIKE, THINK, COULD, NEST);

        for (var sp : splits) {
            var onlyC3 = sp == AM || sp == COULD || sp == WHETHER || sp == IS || sp == LIKE || sp == POSSIBILITY || sp == THINK || sp == NEST || sp == WHAT;
            var hasC3 = sp == BETTER || onlyC3;

            if (isPerfectMatch(sp.pattern, s, hasC3, onlyC3)) {
                split = sp;

                var matcher = sp.pattern.matcher(s);
                if (! matcher.find()) {
                    continue;
                }

                left = matcher.group("m1");
                right = matcher.group("m2");

                if (sp == WHETHER) {
                    is = matcher.group("c3");
                    not = matcher.group("m3");
                    if (! StringUtils.hasText(left)) left = "...";
                    if (! StringUtils.hasText(right)) right = "";

                    try {
                        var is2 = matcher.group("c2");
                        //要不要，如果不是ABA那么不能匹配
                        if (! Objects.equals(is2, is)) {
                            continue;
                        }
                        //找不到也不行
                    } catch (IllegalArgumentException | IllegalStateException e) {
                        continue;
                    }
                }

                if (sp == COULD) {
                    is = matcher.group("c3");
                    not = "不";
                    if (! StringUtils.hasText(left)) left = "...";
                    if (! StringUtils.hasText(right)) right = "";
                }

                if (sp == POSSIBILITY) {
                    num = Math.round(getRandom(1) * 10000f) / 100f;
                }

                if (sp == LIKE) {
                    is = matcher.group("c3");
                }

                if (sp == RANGE) {
                    int range;

                    try {
                        range = Integer.parseInt(right);
                    } catch (NumberFormatException e) {
                        range = 100;
                    }

                    if (range <= 0) {
                        throw new DiceException(DiceException.Type.DICE_Number_TooSmall);
                    } else if (range <= 100) {
                        num = getRandom(100);
                    } else if (range <= 10000) {
                        num = getRandom(10000);
                    } else if (range <= 1000000) {
                        num = getRandom(1000000);
                    } else {
                        throw new DiceException(DiceException.Type.DICE_Number_TooLarge);
                    }
                }

                //排除掉AB一样的选择要求
                if (StringUtils.hasText(left) && StringUtils.hasText(right) && num == 0f) {
                    boolean m = false;
                    try {
                        m = (left.toLowerCase().contains(right.toLowerCase()) || right.toLowerCase().contains(left.toLowerCase())) && left.length() >= 3 && right.length() >= 3;
                    } catch (PatternSyntaxException ignored) {

                    }

                    if (m) {
                        throw new DiceException(DiceException.Type.DICE_Compare_NoDifference);
                    }
                }

                break;
            }
        }

        if (Objects.nonNull(split) && Objects.nonNull(left) && Objects.nonNull(right)) {
            leftFormat = switch (split) {
                case MULTIPLE -> "要我选的话，我觉得，%s。";
                case NEST -> "你搁这搁这呢？";

                case AM -> "我是 Yumu 机器人。";
                case POSSIBILITY -> "概率是：%.2f%%";
                case RANGE -> "您许愿的结果是：%.0f。";

                case WHAT -> "我不知道。";
                case BETTER, COMPARE, OR, JUXTAPOSITION, PREFER, HESITATE, EVEN -> "当然%s啦！";
                case ASSUME -> "%s。";
                case COULD, WHETHER -> "%s%s%s。";
                case CONDITION, IS -> "是的。";
                case LIKE -> "%s。";
                case THINK -> "嗯。";
            };

            rightFormat = switch (split) {
                case MULTIPLE -> "要我选的话，我觉得，%s。";
                case NEST -> "你搁这搁这呢？";

                case AM -> "我是你爹。";
                case POSSIBILITY -> "概率是：%.2f%%";
                case RANGE -> "您许愿的结果是：%.0f。";

                case WHAT -> "是哈基米。\n整个宇宙都是哈基米组成的。";
                case BETTER, OR, JUXTAPOSITION, PREFER, HESITATE, COMPARE -> "当然%s啦！";
                case EVEN -> "当然不%s啦！";
                case ASSUME -> "没有如果。";
                case COULD, WHETHER -> "%s%s%s%s。"; //他 不 是 猪。
                case CONDITION, IS -> "不是。";
                case LIKE -> "不%s。";
                case THINK -> "也没有吧。";
            };

            //改变几率
            boundary = switch (split) {
                case PREFER -> 0.35f; //更喜欢A
                case HESITATE -> 0.65f; //更喜欢B
                case EVEN -> 0.7f; //需要鼓励去B
                case WHAT, AM -> 0.9f; //我是哈基米。 //我是你爹
                default -> 0.5f;
            };
        } else {
            try {
                return chooseMultiple(s);
            } catch (Exception e) {
                log.info(STR."扔骰子：\{s} 匹配失败。");
                throw new DiceException(DiceException.Type.DICE_Compare_NotMatch);
            }
        }

        //更换主语和和谐
        {
            left = ChangeCase(left);
            right = ChangeCase(right);
        }

        //如果还是有条件。那么进入多匹配模式。
        {
            var leftHas = MULTIPLE.pattern.matcher(left);
            var rightHas = MULTIPLE.pattern.matcher(right);

            if (leftHas.find() && StringUtils.hasText(leftHas.group("m2")) || rightHas.find() && StringUtils.hasText(rightHas.group("m2"))) {
                return chooseMultiple(s); //LR一样的
            }
        }

        if (result < boundary - 0.002f) {
            //选第一个
            switch (split) {
                case AM -> {
                    if (StringUtils.hasText(right)) {
                        return STR."\{leftFormat}不是\{right}。";
                    }
                    return leftFormat;
                }

                case WHAT, CONDITION, THINK, NEST, IS -> {
                    return leftFormat;
                }
                case RANGE, POSSIBILITY -> {
                    return String.format(leftFormat, num);
                }
                case BETTER, COMPARE, JUXTAPOSITION, PREFER, HESITATE-> {
                    return String.format(leftFormat, left);
                }
                //注意，这个会忽视A
                case ASSUME, EVEN -> {
                    return String.format(leftFormat, right);
                }
                case COULD, WHETHER -> {
                    return String.format(leftFormat, left, is, right);
                }
                case LIKE -> {
                    return String.format(leftFormat, is);
                }
                case OR -> {
                    if (left.contains("是")) {
                        leftFormat = "我觉得，%s。";
                    }
                    return String.format(leftFormat, left);
                }
            }
        } else if (result > boundary + 0.002f) {
            //选第二个
            switch (split) {
                case WHAT, AM, ASSUME, CONDITION, THINK, NEST, IS -> {
                    return rightFormat;
                }
                case RANGE, POSSIBILITY -> {
                    return String.format(rightFormat, num);
                }
                case BETTER, COMPARE, JUXTAPOSITION, PREFER, HESITATE, EVEN -> {
                    return String.format(rightFormat, right);
                }
                case OR -> {
                    if (right.contains("是")) {
                        rightFormat = "我觉得，%s。";
                    }
                    return String.format(rightFormat, right);
                }
                case COULD, WHETHER -> {
                    return String.format(rightFormat, left, not, is, right);
                }
                case LIKE -> {
                    return String.format(rightFormat, is);
                }
            }
        } else {
            //打平机会千分之四。彩蛋？
            if (result > boundary + 0.001f) {
                throw new DiceException(DiceException.Type.DICE_Compare_All);
            } else {
                throw new DiceException(DiceException.Type.DICE_Compare_Tie);
            }
        }

        log.error("扔骰子：不正常结束！");
        throw new DiceException(DiceException.Type.DICE_Compare_Wtf);
    }

    enum Split {
        //用于匹配是否还有关联词
        MULTIPLE(Pattern.compile("(?<m1>[\\u4e00-\\u9fa5\\w\\s.\\-_]*)?(还是|或者?是?|与)(?<m2>[\\u4e00-\\u9fa5\\w\\s.\\-_]*)")),

        NEST(Pattern.compile("(?<m1>[\\u4e00-\\u9fa5\\w\\s.\\-_]*)?(?<c3>[!！]d)(?<m2>[\\u4e00-\\u9fa5\\w\\s.\\-_]*)?")),

        POSSIBILITY(Pattern.compile("(?<m1>[\\u4e00-\\u9fa5\\w\\s.\\-_]*)?(?<c3>((有多[少大])?的?(概率是?|可能[是性]?))|\\s(chance|possib(l[ey]|ility)(\\sis)?)\\s)(?<m2>[\\u4e00-\\u9fa5\\w\\s.\\-_]*)?")),

        RANGE(Pattern.compile("(?<m1>[大多高等小少低]于(等于)?|约等于?|超过|不足|多少|[><]=?|[＞＜≥≤≡≈]|\\s(more|less)\\s(than)?\\s)(?<c3>[\\u4e00-\\u9fa5\\w\\s.\\-_]*?)?\\s*(?<m2>\\d+)")),

        //是不是
        //A是。A不是。
        WHETHER(Pattern.compile("\\s*(?<m1>[\\u4e00-\\u9fa5\\w\\s.\\-_]*)?\\s*(?<c2>[\\u4e00-\\u9fa5\\w\\s.\\-_])(?<m3>[不没])(?<c3>[\\u4e00-\\u9fa5\\w\\s.\\-_])[个位条只匹头颗根]?\\s*(?<m2>[\\u4e00-\\u9fa5\\w\\s.\\-_]*)?")),

        //A和B比谁更C？
        //正常选择
        //当然选 X 啦！
        BETTER(Pattern.compile("\\s*(?<m1>[\\u4e00-\\u9fa5\\w\\s.\\-_]*)\\s*(?<c2>(跟|和|与|并|\\s(and|or|with)\\s))\\s*(?<m2>[\\u4e00-\\u9fa5\\w\\s.\\-_]*?)\\s*比?(比[，,\\s]*?哪个|比[，,\\s]*?谁|哪个|谁)更?(?<c3>[\\u4e00-\\u9fa5\\w\\s.\\-_]*)")),

        //A比B厉害？
        //正常选择
        //当然选 A 啦！，当然是 B 啦！（B厉害，这里分不开）
        COMPARE(Pattern.compile("\\s*(?<m1>[\\u4e00-\\u9fa5\\w\\s.\\-_]*)[，,\\s]*?(?<c2>(比(?![赛如比拟重邻值及照目价例试上下肩方对分热画划类舍武翼意义喻作基利天推量年萨勒葫芦集速时势特体]|$)较?|(\\scompare(\\sto)?\\s)))[，,\\s]*?(?<m2>[\\u4e00-\\u9fa5\\w\\s.\\-_]*)")),

        //选A，还是选B？
        //正常选择
        //当然选 X 啦！
        OR(Pattern.compile("\\s*(?<c1>(不?是|要么|是要?)(选?[择中好]?了?)?)?\\s*(?<m1>[\\u4e00-\\u9fa5\\w\\s.\\-_]*)[，,\\s]*?(?<c2>([：:]|[还就而]是|and|or|或|或者|要么)(选?[择中好]?了?)?)\\s*(?<m2>[\\u4e00-\\u9fa5\\w\\s.\\-_]*)")),

        //你谁？
        //我是 YumuBot
        AM(Pattern.compile("(?<m1>[\\u4e00-\\u9fa5\\w\\s.\\-_]*)?(?<c3>你是谁?)(?<m2>[\\u4e00-\\u9fa5\\w\\s.\\-_]*)?")),

        //是什么？
        //我不知道。是哈基米。
        WHAT(Pattern.compile("\\s*(?<m1>[\\u4e00-\\u9fa5\\w\\s.\\-_]*)?\\s*(?<c3>(?<!你们?|[要还那])是(([你我他她它祂]们?|别人)?谁|哪[个里处位天日]|什么|啥))\\s*(?<m2>[\\u4e00-\\u9fa5\\w\\s.\\-_]*)?")),

        //是吗？
        //是的。不是。
        IS(Pattern.compile("\\s*(?<m1>[\\u4e00-\\u9fa5\\w\\s.\\-_]*?)?\\s*?(?<c3>(?<![要还那])是吗?|\\sis\\s)\\s*?(?<m2>[\\u4e00-\\u9fa5\\w\\s.\\-_]*)?")),

        //并列AB
        //当然选 X 啦！
        JUXTAPOSITION(Pattern.compile("\\s*(?<c1>(不仅|一边|一方面|有时|既)(选?[择中好]?了?)?)\\s*(?<m1>[\\u4e00-\\u9fa5\\w\\s.\\-_]*)[，,\\s]*?(?<c2>(而且|一边|一方面|有时|又)(选?[择中好]?了?)?)\\s*(?<m2>[\\u4e00-\\u9fa5\\w\\s.\\-_]*)")),

        //宁可A，也不B
        //偏好A
        //当然选 X 啦！
        PREFER(Pattern.compile("\\s*(?<c1>(宁[可愿]|尽管)(选?[择中好]?了?)?)?\\s*(?<m1>[\\u4e00-\\u9fa5\\w\\s.\\-_]*)[，,\\s]*?(?<c2>(也不[要想]?(选?[择中好]?了?)?))\\s*(?<m2>[\\u4e00-\\u9fa5\\w\\s.\\-_]*)")),

        //与其A，不如B
        //偏好B
        //当然选 X 啦！
        HESITATE(Pattern.compile("\\s*(?<c1>(与其|虽然|尽管)(选?[择中好]?了?)?)?\\s*(?<m1>[\\u4e00-\\u9fa5\\w\\s.\\-_]*)[，,\\s]*?(?<c2>(还?不如|比不上|但是|可是|然而|却)(选?[择中好]?了?)?)\\s*(?<m2>[\\u4e00-\\u9fa5\\w\\s.\\-_]*)")),

        //即使A，也B
        //偏好当然
        //当然B，不会B。
        EVEN(Pattern.compile("\\s*(?<c1>(即使|\\seven\\sif\\s)((选?[择中好]?了?)?[择中好])?)?\\s*(?<m1>[\\u4e00-\\u9fa5\\w\\s.\\-_]*)[，,\\s]*?([你我他她它祂]们?|别人)?(?<c2>([也还]会?)(选?[择中好]?了?)?)\\s*(?<m2>[\\u4e00-\\u9fa5\\w\\s.\\-_]*)")),

        //假设A，才有B。
        //我觉得 A 也没啥。// 没有如果。
        ASSUME(Pattern.compile("\\s*(?<c1>(如果|假使|假设|要是|\\s(if|assume)\\s))\\s*(?<m1>[\\u4e00-\\u9fa5\\w\\s.\\-_]*?)[，,\\s]*?(?<c2>(那?([你我他她它祂]们?|别人)?[会要想就便么才])|([想要]|想要|能够?|可以))\\s*(?<m2>([你我他她它祂]们?|别人)?[\\u4e00-\\u9fa5\\w\\s.\\-_]*)")),

        //我能

        COULD(Pattern.compile("\\s*(?<m1>[\\u4e00-\\u9fa5\\w\\s.\\-_]*?)\\s*?(?<c2>不)?\\s*?(?<c3>([想要]|想要|能够?|可以))\\s*(?<m2>[\\u4e00-\\u9fa5\\w\\s.\\-_]*?)")),

        //A是B？
        //确实。 //不对。
        CONDITION(Pattern.compile("\\s*(?<c1>(只要|只有|无论|不管|忽略|忽视|不(去)?想|\\sif\\s))\\s*(?<m1>[\\u4e00-\\u9fa5\\w\\s.\\-_]*)[，,\\s]*?(?<c2>(([你我他她它祂]们?|别人)?[就才都也还]是?|反正|依然))\\s*(?<m2>[\\u4e00-\\u9fa5\\w\\s.\\-_]*)")),

        LIKE(Pattern.compile("\\s*(?<m1>[\\u4e00-\\u9fa5\\w\\s.\\-_]*?)?\\s*?(?<c3>喜欢|爱|\\s((dis)?like|love)\\s)\\s*?(?<m2>[\\u4e00-\\u9fa5\\w\\s.\\-_]*)?")),

        //觉得
        //嗯。也没有吧。
        THINK(Pattern.compile("\\s*(?<m1>[\\u4e00-\\u9fa5\\w\\s.\\-_]*)?\\s*(?<c2>[\\u4e00-\\u9fa5\\w\\s.\\-_])(?<c3>(觉得|认为))\\s*(?<m2>[\\u4e00-\\u9fa5\\w\\s.\\-_]*)?")),

        ;

        public final Pattern pattern;

        Split(Pattern p) {
            this.pattern = p;
        }
    }

    private String chooseMultiple(String s) throws DiceException {
        String[] strings = s.split("还是|或者?是?|[是或与,，.。/?!、？！:：]|\\s+");
        List<String> stringList = Arrays.stream(strings).filter(StringUtils::hasText).toList();

        if (stringList.isEmpty() || stringList.size() == 1) {
            throw new DiceException(DiceException.Type.DICE_Compare_NotMatch);
        }

        var r = Math.round(getRandom(stringList.size()) - 1);
        return String.format("当然%s啦！", ChangeCase(stringList.get(r))); //lr format一样的
    }

    private boolean isPerfectMatch(Pattern p, String s, boolean hasC3, boolean onlyC3) {
        var matcher = p.matcher(s);

        if (! matcher.find()) {
            return false;
        }

        var m1 = Objects.nonNull(matcher.group("m1")) && StringUtils.hasText(matcher.group("m1"));
        var m2 = Objects.nonNull(matcher.group("m2")) && StringUtils.hasText(matcher.group("m2"));
        var c3 = hasC3 && Objects.nonNull(matcher.group("c3")) && StringUtils.hasText(matcher.group("c3"));

        if (onlyC3) return c3;
        if (hasC3) return m1 && m2 && c3;
        return m1 && m2;
    }

    /**
     * 获取随机数
     * @param range 范围
     * @return 如果范围是 1，返回 1。如果范围大于 1，返回 1-范围内的数（Float 的整数），其他则返回 0-1。
     * @param <T> 数字的子类
     */
    public <T extends Number> float getRandom(@Nullable T range) {
        long millis = System.currentTimeMillis() % 1000;
        int r;

        try {
            r = Integer.parseInt(String.valueOf(range));
        } catch (NumberFormatException e) {
            try {
                r = Math.round((float) range);
            } catch (NumberFormatException e1) {
                return millis / 999f;
            }
        }

        if (r > 1) {
            return Math.round(millis / 999f * (r - 1)) + 1f;
        } else {
            return millis / 999f;
        }
    }

    /**
     * 改变主宾格，删除语气助词，和谐违禁词
     * @param s 未和谐
     * @return 和谐
     */
    private String ChangeCase(String s) {
        return s.trim()
                .replaceAll("你们?", "雨沐")
                .replaceAll("(?i)\\syours?\\s", " yumu's ")
                .replaceAll("(?i)\\syou\\s", " yumu ")
                .replaceAll("我们", "你们")
                .replaceAll("我", "你")
                .replaceAll("(?i)\\s([Ii]|me)\\s", " you ")
                .replaceAll("(?i)\\smy\\s", " your ")
                .replaceAll("(?i)\\smine\\s", " yours ")

                .replaceAll("[阿啊呃欸哇呀哟欤呕噢呦嘢哦吧呗啵啦嘞哩咧咯啰喽吗嘛嚜哪呢呐呵兮噻哉矣焉]|[哈罢否乎么麽][?？!！。.\\s]?$", "") //耶来唻了价也罗给的般则连不呸 不匹配，删去其他语气助词

                .replaceAll("[习習]近平|[习習]?总书记|主席|国家|政治|反动|反?共(产党)?|[国國]民[党黨]|天安门|极[左右](主义)?|革命|(社会)?主义|情趣|迪克|高潮|色色|[蛇射受授吞]精|潮喷|成人|性交|男娘|做爱|后入|药娘|怀孕|生殖器|寄吧|几把|鸡[鸡巴]|[精卵]子|[精爱]液|子宫|阴[茎蒂唇囊道]|[阴吊叼批肛]毛|搞基|出?脚本|[Rr]-?18", "(和谐)")
                .replaceAll("[黨党吊批逼操肏肛杀穴屁萎猥]", "○");
    }
}
