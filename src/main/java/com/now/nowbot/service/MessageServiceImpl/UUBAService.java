package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.Service.UserParam;
import com.now.nowbot.model.enums.Mod;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.model.imag.MapAttrGet;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.qq.message.AtMessage;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuScoreApiService;
import com.now.nowbot.service.OsuApiService.OsuUserApiService;
import com.now.nowbot.throwable.ServiceException.BPAnalysisException;
import com.now.nowbot.throwable.ServiceException.BindException;
import com.now.nowbot.util.QQMsgUtil;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;
import java.util.regex.Pattern;

@Service("UUBA")
public class UUBAService implements MessageService<UUBAService.BPHeadTailParam> {
    OsuUserApiService userApiService;
    OsuScoreApiService scoreApiService;
    BindDao bindDao;
    ImageService imageService;

    //bpht 的全称大概是 BP Head / Tail
    public record BPHeadTailParam(UserParam user, boolean info){}

    @Autowired
    public UUBAService(OsuUserApiService userApiService, OsuScoreApiService scoreApiService, BindDao bindDao, ImageService imageService) {
        this.userApiService = userApiService;
        this.scoreApiService = scoreApiService;
        this.bindDao = bindDao;
        this.imageService = imageService;
    }

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

    static final Pattern pattern = Pattern.compile("^[!！]\\s*(?i)(uubpanalysis|u(u)?(ba|bpa))(?<info>(-?i))?(\\s*[:：](?<mode>[\\w\\d]+))?(\\s+(?<name>[0-9a-zA-Z\\[\\]\\-_ ]{3,}))?");
    static final Pattern pattern2 = Pattern.compile("^[!！]\\s*(?i)(ym)?(?<bpht>(bpht))[\\w-]*");

    @Override
    public boolean isHandle(MessageEvent event, DataValue<BPHeadTailParam> data) throws BPAnalysisException {


        //旧功能指引
        var matcher2 = pattern2.matcher(event.getRawMessage().trim());
        if (matcher2.find() && Strings.isNotBlank(matcher2.group("bpht"))) {
            // 直接在这里抛, 效果一样
            throw new BPAnalysisException(BPAnalysisException.Type.BPA_BPHT_NotSupported);
        }

        var matcher = pattern.matcher(event.getRawMessage().trim());
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
        BinUser binUser;

        // 是否为绑定用户
        if (Objects.nonNull(param.user().qq())) {
            try {
                binUser = bindDao.getUserFromQQ(param.user().qq());
            } catch (BindException e) {
                if (!param.user().at()) {
                    throw new BPAnalysisException(BPAnalysisException.Type.BPA_Me_TokenExpired);
                } else {
                    throw new BPAnalysisException(BPAnalysisException.Type.BPA_Player_TokenExpired);
                }
            }
        } else {
            //查询其他人 [name]
            String name = param.user().name();
            long id = 0;
            try {
                id = userApiService.getOsuId(name);
                binUser = bindDao.getUserFromOsuid(id);
            } catch (BindException e) {
                //构建只有 name + id 的对象, binUser == null
                binUser = new BinUser();
                binUser.setOsuID(id);
                binUser.setOsuName(name);
            } catch (Exception e) {
                throw new BPAnalysisException(BPAnalysisException.Type.BPA_Player_NotFound);
            }
        }

        //bp列表
        List<Score> bps;
        //分别处理mode
        var mode = param.user().mode();
        //处理默认mode
        if (mode == OsuMode.DEFAULT && binUser.getMode() != null) mode = binUser.getMode();
        try {
            bps = scoreApiService.getBestPerformance(binUser, mode, 0, 100);
        } catch (WebClientResponseException.BadRequest e) {
            // 请求失败 超时/断网
            if (param.user().qq() == event.getSender().getId()) {
                throw new BPAnalysisException(BPAnalysisException.Type.BPA_Me_TokenExpired);
            } else {
                throw new BPAnalysisException(BPAnalysisException.Type.BPA_Player_TokenExpired);
            }
        } catch (WebClientResponseException.Unauthorized e) {
            // 未绑定
            throw new BPAnalysisException(BPAnalysisException.Type.BPA_Me_TokenExpired);
        }

        if (bps == null || bps.size() <= 5) {
            if (!param.user().at() && Objects.isNull(param.user().name())) {
                throw new BPAnalysisException(BPAnalysisException.Type.BPA_Me_NotEnoughBP);
            } else {
                throw new BPAnalysisException(BPAnalysisException.Type.BPA_Player_NotEnoughBP);
            }
        }

        String[] Lines;
        if (param.info()) {
            if (mode == null || mode == OsuMode.DEFAULT) {
                Lines = getAllMsgI(bps, binUser.getOsuName(), OsuMode.DEFAULT);
            } else {
                Lines = getAllMsgI(bps, binUser.getOsuName(), mode);
            }
        } else {
            if (mode == null || mode == OsuMode.DEFAULT) {
                Lines = getAllMsg(bps, binUser.getOsuName(), "");
            } else {
                Lines = getAllMsg(bps, binUser.getOsuName(), mode.getName());
            }
        }
        QQMsgUtil.sendImage(from, imageService.drawLine(Lines));
    }

    public String[] getAllMsg(List<Score> bps, String name, String mode) {
        var dtbf = new StringBuffer()
                .append(name).append('[').append(mode).append(']').append('\n');
        double allPP = 0;
        int sSum = 0;
        int xSum = 0;
        int fcSum = 0;
        TreeMap<String, intValue> modeSum = new TreeMap<>(); //各个mod的数量

        DecimalFormat decimalFormat = new DecimalFormat("0.00"); //acc格式
        for (int i = 0; i < bps.size(); i++) {
            var bp = bps.get(i);
            //显示前五跟后五的数据
            if (i < 5 || i > bps.size() - 5) {
                dtbf.append("#")
                        .append(i + 1)
                        .append(' ')
                        .append(decimalFormat.format(bp.getPP()))
                        .append(' ')
                        .append(decimalFormat.format(100 * bp.getAccuracy()))
//                        .append(decimalFormat.format(accCoun.getAcc(bp)))
                        .append('%')
                        .append(' ')
                        .append(bp.getRank());
                if (!bp.getMods().isEmpty()) {
                    for (int j = 0; j < bp.getMods().size(); j++) {
                        dtbf.append(' ').append(bp.getMods().get(j));
                    }
                }
                dtbf.append('\n');
            } else if (i == 50) {
                dtbf.append("-------分割线-------\n");
            }
            allPP += bp.getPP(); //统计总数
            if (!bp.getMods().isEmpty()) {
                for (int j = 0; j < bp.getMods().size(); j++) {
                    String mod = bp.getMods().get(j);
                    if (!modeSum.containsKey(mod)) modeSum.put(mod, new intValue());
                    else modeSum.get(mod).add();
                }
            }
            if (bp.getRank().contains("S")) sSum++;
            if (bp.getRank().contains("X")) {
                sSum++;
                xSum++;
            }
            if (bp.isPerfect()) fcSum++;
        }
        dtbf.append("累计模组有：\n");
        modeSum.forEach((mod, sum) -> dtbf.append(mod).append(' ').append(sum.value).append(';'));
        dtbf.append("\nBP中有").append(sSum).append("个 S+ 评级\n满连击的 FC 成绩有").append(fcSum).append('个');
        if (xSum != 0) dtbf.append("\n其中 SS 有").append(xSum).append('个');
        dtbf.append("\nBP 的 PP 差为").append(decimalFormat.format(bps.get(0).getPP() - bps.get(bps.size() - 1).getPP()));
        dtbf.append("\nBP 的平均 PP 为").append(decimalFormat.format(allPP / bps.size()));

        return dtbf.toString().split("\n");
    }

    public String[] getAllMsgI(List<Score> bps, String name, OsuMode mode) {
        if (bps.isEmpty()) return new String[0];
        var sb = new StringBuffer()
                .append(name).append('[').append(mode).append(']').append('\n');

        var BP1 = bps.get(0);
        var BP1BPM = BP1.getBeatMap().getBPM();
        float BP1Length = BP1.getBeatMap().getTotalLength();
        if (BP1.getMods().contains("DT") || BP1.getMods().contains("NC")) {
            BP1Length /= 1.5f;
            BP1BPM *= 1.5f;
        } else if (BP1.getMods().stream().anyMatch(r -> r.equals("HT"))) {
            BP1Length /= 0.75f;
            BP1BPM *= 0.75f;
        }
        float star = 0f;
        float maxBPM = BP1BPM;
        int maxCombo = 0;
        int maxComboValue = BP1.getMaxCombo();
        int maxLength = 0;
        float maxLengthValue = BP1Length;

        float minBPM = maxBPM;
        int minCombo = 0;
        int minComboValue = maxComboValue;
        int minLength = 0;
        float minLengthValue = maxLengthValue;

        float avgLength = 0f;
        int avgCombo = 0;
        int maxTimeToPp = 0;
        float maxTimeToPpValue = 0f;
        float allPP = 0f;
        float nowPP = 0f;

        TreeMap<String, modData> modSum = new TreeMap<>(); //各个mod的数量

        TreeMap<Long, mapperData> mapperSum = new TreeMap<>();
        DecimalFormat decimalFormat = new DecimalFormat("0.00"); //acc格式

        var mapAttrGet = new MapAttrGet(mode);
        bps.stream()
                .peek(s -> {
                    if (s.getMods().isEmpty()) s.setScore(0);
                    int f = s.getMods().stream().map(Mod::fromStr).map(m1 -> m1.value).reduce(0, (id, s1) -> s1 | id);
                    s.setScore(f);
                })
                .filter(s -> Mod.hasChangeRating(s.getScore()))
                .forEach(s -> mapAttrGet.addMap(s.getScoreId(), s.getBeatMap().getId(), s.getScore()));
        var changedStarMap = imageService.getMapAttr(mapAttrGet);
        for (int i = 0; i < bps.size(); i++) {
            var bp = bps.get(i);
            var map = bp.getBeatMap();
            float length = map.getTotalLength();
            float bpm = map.getBPM();
            bp.getMods().forEach(r -> {
                if (modSum.containsKey(r)) {
                    modSum.get(r).add(bp.getWeight().getPP());
                } else {
                    modSum.put(r, new modData(bp.getWeight().getPP()));
                }
            });
            if (bp.getMods().contains("DT") || bp.getMods().contains("NC")) {
                length /= 1.5f;
                bpm *= 1.5f;
            } else if (bp.getMods().stream().anyMatch(r -> r.equals("HT"))) {
                length /= 0.75f;
                bpm *= 0.75f;
            }

            avgLength += length;

            if (changedStarMap.containsKey(bp.getScoreId())) {
                star += changedStarMap.get(bp.getScoreId()).getStars();
            } else {
                star += bp.getBeatMap().getDifficultyRating();
            }

            if (bpm < minBPM) {
                minBPM = bpm;
            } else if (bpm > maxBPM) {
                maxBPM = bpm;
            }

            if (length < minLengthValue) {
                minLength = i;
                minLengthValue = length;
            } else if (length > maxLengthValue) {
                maxLength = i;
                maxLengthValue = length;
            }

            if (bp.getMaxCombo() < minComboValue) {
                minCombo = i;
                minComboValue = bp.getMaxCombo();
            } else if (bp.getMaxCombo() > maxComboValue) {
                maxCombo = i;
                maxComboValue = bp.getMaxCombo();
            }
            avgCombo += bp.getMaxCombo();

            float tthToPp = (bp.getPP()) / (map.getSliders() + map.getSpinners() + map.getCircles());
            if (maxTimeToPpValue < tthToPp) {
                maxTimeToPp = i;
                maxTimeToPpValue = tthToPp;
            }

            if (mapperSum.containsKey(map.getUserId())) {
                mapperSum.get(map.getUserId()).add(bp.getPP());
            } else {
                mapperSum.put(map.getUserId(), new mapperData(bp.getPP(), map.getUserId()));
            }
            nowPP += bp.getWeight().getPP();
            allPP += bp.getPP();
        }
        avgCombo /= bps.size();
        avgLength /= bps.size();
        star /= bps.size();

        sb.append("BP 平均长度: ").append(getTimeStr((int) avgLength)).append('\n');
        sb.append("最长是 BP").append(maxLength + 1).append(' ').append(getTimeStr((int) maxLengthValue)).append('\n');
        sb.append("最短是 BP").append(minLength + 1).append(' ').append(getTimeStr((int) minLengthValue)).append('\n');

        sb.append("BP 平均连击: ").append(avgCombo).append('\n');
        sb.append("BP 平均星级: ").append(String.format("%.2f", star)).append('\n');
        sb.append("Combo 最大是 BP").append(maxCombo + 1).append(' ').append(maxComboValue).append('x').append('\n');
        sb.append("Combo 最小是 BP").append(minCombo + 1).append(' ').append(minComboValue).append('x').append('\n');

        sb.append("单图 PP/TTH 比例最大的是 BP").append(maxTimeToPp + 1)
                .append(" 获得").append(decimalFormat.format(maxTimeToPpValue)).append("PP/TTH").append('\n');

        sb.append("BPM 统计:").append(String.format("%.0f", minBPM)).append('-').append(String.format("%.0f", maxBPM)).append('\n');

        sb.append("BP Mapper 统计:\n");
        var mappers = mapperSum.values().stream()
                .sorted((o1, o2) -> {
                    if (o1.size != o2.size) return 2 * (o2.size - o1.size);
                    return o1.allPP > o2.allPP ? -1 : 1;
                })
                .limit(9).toList();
        var mappersId = mappers.stream().map(u -> u.uid).toList();
        var mappersInfo = userApiService.getUsers(mappersId);
        var mapperIdToInfo = new HashMap<Long, String>();
        for (var node : mappersInfo) {
            mapperIdToInfo.put(node.getId(), node.getUserName());
        }
        mappers.forEach(mapperDate -> {
            try {
                sb.append(mapperIdToInfo.get(mapperDate.uid)).append(' ').append(mapperDate.size).append("个 ")
                        .append(decimalFormat.format(mapperDate.allPP)).append("PP").append('\n');
            } catch (Exception e) {
                sb.append("id为").append(mapperDate.uid).append("暂未找到，但是有").append(mapperDate.size).append("个 总计")
                        .append(decimalFormat.format(mapperDate.allPP)).append("PP").append('\n');
            }
        });
        sb.append("累计模组有:\n");
        float finalAllPP = nowPP;
        modSum.forEach((mod, sum) -> sb.append(mod).append('*').append(sum.size).append(' ').append("总计")
                .append(decimalFormat.format(sum.getAllPP()))
                .append('[').append(decimalFormat.format(100 * sum.getAllPP() / finalAllPP)).append('%').append(']')
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

        mapperData add(float pp) {
            allPP += pp;
            size++;
            return this;
        }

        public float getAllPP() {
            return allPP;
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

        modData add(float pp) {
            allPP += pp;
            size++;
            return this;
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
            return l + "秒";
        } else {
            return l / 60 + "分" + l % 60 + "秒";
        }
    }
}


