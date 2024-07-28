package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.Service.UserParam;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.qq.message.AtMessage;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService;
import com.now.nowbot.service.OsuApiService.OsuScoreApiService;
import com.now.nowbot.service.OsuApiService.OsuUserApiService;
import com.now.nowbot.throwable.ServiceException.BPAnalysisException;
import com.now.nowbot.throwable.ServiceException.BindException;
import com.now.nowbot.util.HandleUtil;
import com.now.nowbot.util.Instruction;
import com.now.nowbot.util.Instructions;
import com.now.nowbot.util.QQMsgUtil;
import jakarta.annotation.Resource;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service("UU_BA")
public class UUBAService implements MessageService<UUBAService.BPHeadTailParam> {
    private static final Logger log = LoggerFactory.getLogger(UUBAService.class);
    @Resource
    OsuUserApiService userApiService;
    @Resource
    OsuScoreApiService   scoreApiService;
    @Resource
    OsuBeatmapApiService beatmapApiService;
    @Resource
    BindDao              bindDao;
    @Resource
    ImageService imageService;

    //bpht 的全称大概是 BP Head / Tail
    public record BPHeadTailParam(UserParam user, boolean info){}

    static class intValue {
        int value = 1;

        public intValue add() {
            value++;
            return this;
        }

        public int value() {
            return value;
        }
    }

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<BPHeadTailParam> data) throws BPAnalysisException {
        //旧功能指引
        var matcher2 = Instructions.DEPRECATED_BPHT.matcher(messageText);
        if (matcher2.find() && Strings.isNotBlank(matcher2.group("bpht"))) {
            throw new BPAnalysisException(BPAnalysisException.Type.BA_Instruction_Deprecated);
        }

        var matcher = Instruction.UU_BA.matcher(messageText);
        if (!matcher.find()) return false;
        boolean info = Strings.isNotBlank(matcher.group("info"));
        var mode = OsuMode.getMode(matcher.group("mode"));
        var at = QQMsgUtil.getType(event.getMessage(), AtMessage.class);

        if (Objects.nonNull(at)) {
            data.setValue(new BPHeadTailParam(
                    new UserParam(at.getTarget(), null, mode, true), info));
            return true;
        }
        String name = matcher.group("name");
        if (Objects.nonNull(name) && Strings.isNotBlank(name)) {
            data.setValue(new BPHeadTailParam(
                    new UserParam(null, name, mode, false), info));
            return true;
        }
        data.setValue(new BPHeadTailParam(
                new UserParam(event.getSender().getId(), null, mode, false), info));
        return true;
    }

    @Override
    public void HandleMessage(MessageEvent event, BPHeadTailParam param) throws Throwable {
        var from = event.getSubject();
        BinUser bu;

        // 是否为绑定用户
        if (Objects.nonNull(param.user().qq())) {
            try {
                bu = bindDao.getUserFromQQ(param.user().qq());
            } catch (BindException e) {
                if (!param.user().at()) {
                    throw new BPAnalysisException(BPAnalysisException.Type.BA_Me_TokenExpired);
                } else {
                    throw new BPAnalysisException(BPAnalysisException.Type.BA_Player_TokenExpired);
                }
            }
        } else {
            //查询其他人 [data]
            String name = param.user().name();
            long id = 0;
            try {
                id = userApiService.getOsuId(name);
                bu = bindDao.getUserFromOsuid(id);
            } catch (BindException e) {
                //构建只有 data + id 的对象, binUser == null
                bu = new BinUser();
                bu.setOsuID(id);
                bu.setOsuName(name);
            } catch (Exception e) {
                throw new BPAnalysisException(BPAnalysisException.Type.BA_Player_NotFound);
            }
        }

        List<Score> bps;

        var mode = HandleUtil.getModeOrElse(param.user.mode(), bu);

        try {
            bps = scoreApiService.getBestPerformance(bu, mode, 0, 100);
        } catch (HttpClientErrorException.BadRequest | WebClientResponseException.BadRequest e) {
            // 请求失败 超时/断网
            if (param.user().qq() == event.getSender().getId()) {
                throw new BPAnalysisException(BPAnalysisException.Type.BA_Me_TokenExpired);
            } else {
                throw new BPAnalysisException(BPAnalysisException.Type.BA_Player_TokenExpired);
            }
        } catch (HttpClientErrorException.Unauthorized | WebClientResponseException.Unauthorized e) {
            // 未绑定
            throw new BPAnalysisException(BPAnalysisException.Type.BA_Me_TokenExpired);
        }

        if (bps == null || bps.size() <= 10) {
            if (!param.user().at() && Objects.isNull(param.user().name())) {
                throw new BPAnalysisException(BPAnalysisException.Type.BA_Me_NotEnoughBP, mode.getName());
            } else {
                throw new BPAnalysisException(BPAnalysisException.Type.BA_Player_NotEnoughBP, mode.getName());
            }
        }

        beatmapApiService.applySRAndPP(bps);

        String[] Lines;
        if (param.info()) {
            if (mode == null || mode == OsuMode.DEFAULT) {
                Lines = getAllMsgI(bps, bu.getOsuName(), OsuMode.DEFAULT);
            } else {
                Lines = getAllMsgI(bps, bu.getOsuName(), mode);
            }
        } else {
            if (mode == null || mode == OsuMode.DEFAULT) {
                Lines = getAllMsg(bps, bu.getOsuName(), "");
            } else {
                Lines = getAllMsg(bps, bu.getOsuName(), mode.getName());
            }
        }

        try {
            var image = imageService.getPanelAlpha(Lines);
            from.sendImage(image);
        } catch (Exception e) {
            throw new BPAnalysisException(BPAnalysisException.Type.BA_Send_UUError);
        }
    }

    public String[] getAllMsg(List<Score> bps, String name, String mode) {
        var sb = new StringBuffer().append(name).append(": ").append(' ').append(mode).append('\n');
        double allPP = 0;
        int sSum = 0;
        int xSum = 0;
        int fcSum = 0;
        TreeMap<String, intValue> modTreeMap = new TreeMap<>(); //各个mod的数量

        for (int i = 0; i < bps.size(); i++) {
            var bp = bps.get(i);
            //显示前五跟后五的数据
            if (i < 5 || i >= bps.size() - 5) {
                sb.append("#")
                        .append(i + 1)
                        .append(' ')
                        .append(String.format("%.2f", bp.getPP()))
                        .append(' ')
                        .append(String.format("%.2f", 100 * bp.getAccuracy()))
                        .append('%')
                        .append(' ')
                        .append(bp.getRank());
                if (!bp.getMods().isEmpty()) {
                    sb.append(" +");
                    for (var m : bp.getMods()) {
                        sb.append(m).append(' ');
                    }
                }
                sb.append('\n');
            } else if (i == 5) {
                sb.append("...").append('\n');
            }
            allPP += bp.getPP(); //统计总数
            if (!bp.getMods().isEmpty()) {
                for (int j = 0; j < bp.getMods().size(); j++) {
                    String mod = bp.getMods().get(j);
                    if (!modTreeMap.containsKey(mod)) modTreeMap.put(mod, new intValue());
                    else modTreeMap.get(mod).add();
                }
            }
            if (bp.getRank().contains("S")) sSum++;
            if (bp.getRank().contains("X")) {
                sSum++;
                xSum++;
            }
            if (bp.isPerfect()) fcSum++;
        }
        sb.append("——————————").append('\n');
        sb.append("模组数量: \n");

        AtomicInteger c = new AtomicInteger();

        modTreeMap.forEach((mod, sum) -> {
            c.getAndIncrement();
            sb.append(mod).append(' ').append(sum.value).append("x; ");
            if (c.get() == 2) {
                c.set(0);
                sb.append('\n');
            }
        });

        sb.append("\nS+ 评级: ").append(sSum);
        if (xSum != 0) sb.append("\n     其中 SS: ").append(xSum);

        sb.append('\n').append("完美 FC: ").append(fcSum).append('\n')
                .append("平均: ").append(String.format("%.2f", allPP / bps.size())).append("PP").append('\n')
                .append("差值: ").append(String.format("%.2f", bps.getFirst().getPP() - bps.getLast().getPP())).append("PP");

        return sb.toString().split("\n");
    }

    public String[] getAllMsgI(List<Score> bps, String name, OsuMode mode) {
        if (bps.isEmpty()) return new String[0];
        var sb = new StringBuffer().append(name).append(": ").append(' ').append(mode).append('\n');

        var BP1 = bps.getFirst();
        var BP1BPM = BP1.getBeatMap().getBPM();
        float BP1Length = BP1.getBeatMap().getTotalLength();

        float star;
        float maxStar = BP1.getBeatMap().getStarRating();
        float minStar = maxStar;
        float maxBPM = BP1BPM;
        float minBPM = maxBPM;
        int maxCombo = BP1.getMaxCombo();
        int minCombo = maxCombo;
        float maxLength = BP1Length;
        float minLength = maxLength;

        int maxComboBP = 0;
        int minComboBP = 0;
        int maxLengthBP = 0;
        int minLengthBP = 0;
        int maxStarBP = 0;
        int minStarBP = 0;

        float avgLength = 0f;
        int avgCombo = 0;
        float avgStar = 0f;

        int maxTTHPPBP = 0;
        float maxTTHPP = 0f;
        float nowPP = 0f;

        TreeMap<String, modData> modSum = new TreeMap<>(); //各个mod的数量

        TreeMap<Long, mapperData> mapperSum = new TreeMap<>();
        DecimalFormat decimalFormat = new DecimalFormat("0.00"); //acc格式

        for (int i = 0; i < bps.size(); i++) {
            var bp = bps.get(i);
            var b = bp.getBeatMap();
            float length = b.getTotalLength();
            float bpm = b.getBPM();
            bp.getMods().forEach(r -> {
                if (modSum.containsKey(r)) {
                    modSum.get(r).add(Optional.ofNullable(bp.getWeightedPP()).orElse(0f));
                } else {
                    modSum.put(r, new modData(Optional.ofNullable(bp.getWeightedPP()).orElse(0f)));
                }
            });

            avgLength += length;
            star =  bp.getBeatMap().getStarRating();
            avgStar += star;

            if (bpm < minBPM) {
                minBPM = bpm;
            } else if (bpm >= maxBPM) {
                maxBPM = bpm;
            }

            if (star < minStar) {
                minStarBP = i;
                minStar = star;
            } else if (star > maxStar) {
                maxStarBP = i;
                maxStar = star;
            }

            if (length < minLength) {
                minLengthBP = i;
                minLength = length;
            } else if (length > maxLength) {
                maxLengthBP = i;
                maxLength = length;
            }

            if (bp.getMaxCombo() < minCombo) {
                minComboBP = i;
                minCombo = bp.getMaxCombo();
            } else if (bp.getMaxCombo() > maxCombo) {
                maxComboBP = i;
                maxCombo = bp.getMaxCombo();
            }
            avgCombo += bp.getMaxCombo();

            float tthToPp = (bp.getPP()) / (b.getSliders() + b.getSpinners() + b.getCircles());
            if (maxTTHPP < tthToPp) {
                maxTTHPPBP = i;
                maxTTHPP = tthToPp;
            }

            if (mapperSum.containsKey(b.getMapperID())) {
                mapperSum.get(b.getMapperID()).add(bp.getPP());
            } else {
                mapperSum.put(b.getMapperID(), new mapperData(bp.getPP(), b.getMapperID()));
            }
            nowPP += bp.getWeightedPP();
        }
        avgCombo /= bps.size();
        avgLength /= bps.size();
        avgStar /= bps.size();

        sb.append("平均时间: ").append(getTimeStr((int) avgLength)).append('\n');
        sb.append("时间最长: BP").append(maxLengthBP + 1).append(' ').append(getTimeStr((int) maxLength)).append('\n');
        sb.append("时间最短: BP").append(minLengthBP + 1).append(' ').append(getTimeStr((int) minLength)).append('\n');
        sb.append("——————————").append('\n');

        sb.append("平均连击: ").append(avgCombo).append('x').append('\n');
        sb.append("连击最大: BP").append(maxComboBP + 1).append(' ').append(maxCombo).append('x').append('\n');
        sb.append("连击最小: BP").append(minComboBP + 1).append(' ').append(minCombo).append('x').append('\n');
        sb.append("——————————").append('\n');

        sb.append("平均星数: ").append(String.format("%.2f", avgStar)).append('*').append('\n');
        sb.append("星数最高: BP").append(maxStarBP + 1).append(' ').append(String.format("%.2f", maxStar)).append('*').append('\n');
        sb.append("星数最低: BP").append(minStarBP + 1).append(' ').append(String.format("%.2f", minStar)).append('*').append('\n');
        sb.append("——————————").append('\n');

        sb.append("PP/TTH 比例最大: BP").append(maxTTHPPBP + 1)
                .append("，为").append(decimalFormat.format(maxTTHPP)).append('倍').append('\n');

        sb.append("BPM 区间: ").append(String.format("%.0f", minBPM)).append('-').append(String.format("%.0f", maxBPM)).append('\n');
        sb.append("——————————").append('\n');

        sb.append("谱师: \n");
        var mappers = mapperSum.values().stream()
                .sorted((o1, o2) -> {
                    if (o1.size != o2.size) return 2 * (o2.size - o1.size);
                    return Float.compare(o2.allPP, o1.allPP);
                })
                .limit(9).toList();
        var mappersId = mappers.stream().map(u -> u.uid).toList();
        var mappersInfo = userApiService.getUsers(mappersId);
        var mapperIdToInfo = new HashMap<Long, String>();
        for (var node : mappersInfo) {
            mapperIdToInfo.put(node.getUserID(), node.getUserName());
        }
        mappers.forEach(mapper -> {
            try {
                sb.append(mapperIdToInfo.get(mapper.uid)).append(": ").append(mapper.size).append("x ")
                        .append(decimalFormat.format(mapper.allPP)).append("PP").append('\n');
            } catch (Exception e) {
                sb.append("UID: ").append(mapper.uid).append(": ").append(mapper.size).append("x ")
                        .append(decimalFormat.format(mapper.allPP)).append("PP").append('\n');
            }
        });
        sb.append("——————————").append('\n');
        sb.append("模组数量: \n");
        float finalAllPP = nowPP;
        modSum.forEach((mod, sum) -> sb.append(mod).append(": ").append(sum.size).append("x ")
                .append(decimalFormat.format(sum.getAllPP())).append("PP ")
                .append('(').append(decimalFormat.format(100 * sum.getAllPP() / finalAllPP)).append('%').append(')')
                .append('\n'));
        return sb.toString().split("\n");
    }

    static class mapperData {
        float allPP;
        int   size;
        long  uid;

        mapperData(float pp, long uid) {
            allPP += pp;
            size = 1;
            this.uid = uid;
        }

        void add(float pp) {
            allPP += pp;
            size++;
        }

        public int getSize() {
            return size;
        }
    }

    static class modData {
        float allPP;
        int   size;

        modData(float pp) {
            allPP += pp;
            size = 1;
        }

        void add(float pp) {
            allPP += pp;
            size++;
        }

        public float getAllPP() {
            return allPP;
        }

        public int getSize() {
            return size;
        }
    }

    String getTimeStr(int l) {
        if (l < 60) {
            return STR."\{l}秒";
        } else {
            return STR."\{l / 60}分\{l % 60}秒";
        }
    }
}


