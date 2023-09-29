package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.enums.Mod;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.model.imag.MapAttr;
import com.now.nowbot.model.imag.MapAttrGet;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.qq.message.AtMessage;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.ServiceException.BPAnalysisException;
import com.now.nowbot.throwable.ServiceException.BindException;
import com.now.nowbot.util.QQMsgUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

@Service("bpht")
public class BphtService implements MessageService {
    private static final String TIPS = "此功能已经有新设计，请使用新面板 -> !ba \n\n";
    OsuGetService osuGetService;
    BindDao bindDao;
    ImageService imageService;

    @Autowired
    public BphtService(OsuGetService osuGetService, BindDao bindDao, ImageService imageService) {
        this.osuGetService = osuGetService;
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

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();
        BinUser binUser = null;
        var at = QQMsgUtil.getType(event.getMessage(), AtMessage.class);
        // 是否为绑定用户
        if (at != null) {
            binUser = bindDao.getUser(at.getTarget());
        }
        if (matcher.group("name") != null && !matcher.group("name").trim().isEmpty()) {
            //查询其他人 bpht [name]
            String name = matcher.group("name").trim();
            long id = osuGetService.getOsuId(name);
            try {
                binUser = bindDao.getUserFromOsuid(id);
            } catch (BindException e) {
                //do nothing
            }
            if (binUser == null) {
                //构建只有 name + id 的对象
                binUser = new BinUser();
                binUser.setOsuID(id);
                binUser.setOsuName(name);
            }
        } else {
            //处理没有参数的情况 查询自身
            binUser = bindDao.getUser(event.getSender().getId());
        }

        if (binUser == null) {
            if (matcher.group("name") == null || matcher.group("name").trim().isEmpty()) {
                throw new BPAnalysisException(BPAnalysisException.Type.BPA_Me_LoseBind);
            } else {
                throw new BPAnalysisException(BPAnalysisException.Type.BPA_Player_NotFound);
            }
        }

        //bp列表
        List<Score> bps = null;
        //分别处理mode
        var mode = OsuMode.getMode(matcher.group("mode"));
        //处理默认mode
        if (mode == OsuMode.DEFAULT && binUser.getMode() != null) mode = binUser.getMode();
        try {
            bps = osuGetService.getBestPerformance(binUser, mode, 0, 100);
        } catch (Exception e) {
            throw new BPAnalysisException(BPAnalysisException.Type.BPA_Player_FetchFailed);
        }

        if (bps == null || bps.size() <= 5) {
            throw new BPAnalysisException(BPAnalysisException.Type.BPA_Player_NotEnoughBP);
        }

        String[] Lines;
        if (matcher.group("info") == null) {
            if (mode == null || mode == OsuMode.DEFAULT) {
                Lines = getAllMsg(bps, binUser.getOsuName(), "");
            } else {
                Lines = getAllMsg(bps, binUser.getOsuName(), mode.getName());
            }
        } else {
            if (mode == null || mode == OsuMode.DEFAULT) {
                Lines = getAllMsg(bps, binUser.getOsuName(), OsuMode.DEFAULT);
            } else {
                Lines = getAllMsg(bps, binUser.getOsuName(), mode);
            }
        }
        QQMsgUtil.sendImage(from, imageService.drawLine(Lines));
//        from.sendMessage(dtbf.toString());
    }

    public String[] getAllMsg(List<Score> bps, String name, String mode) {
        var dtbf = new StringBuffer(TIPS)
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

    public String[] getAllMsg(List<Score> bps, String name, OsuMode mode) {
        if (bps.isEmpty()) return new String[0];
        var sb = new StringBuffer(TIPS)
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
        float star = 0;
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

        int avgLength = 0;
        int avgCombo = 0;
        int maxTimeToPp = 0;
        float maxTimeToPpValue = 0;
        float allPP = 0;
        float nowPP = 0;

        TreeMap<String, modData> modSum = new TreeMap<>(); //各个mod的数量

        TreeMap<Integer, mapperData> mapperSum = new TreeMap<>();
        DecimalFormat decimalFormat = new DecimalFormat("0.00"); //acc格式

        var mapAttrGet = new MapAttrGet(mode);
        bps.stream()
                .peek(s -> {
                    if (s.getMods().isEmpty()) s.setScore(0);
                    int f = s.getMods().stream().map(Mod::fromStr).map(m1 -> m1.value).reduce(0, (id, s1) -> s1 | id);
                    s.setScore(f);
                })
                .filter(s -> Mod.hasChangeRating(s.getScore()))
                .forEach(s -> {
                    mapAttrGet.addMap(s.getBeatMap().getId(), s.getScore());
                });
        var changedStarMapAttrs = imageService.getMapAttr(mapAttrGet);
        var changedStarMap = changedStarMapAttrs.stream().collect(Collectors.toMap(MapAttr::getBid, s -> s));
        for (int i = 0; i < bps.size(); i++) {
            var bp = bps.get(i);
            var map = bp.getBeatMap();
            int length = map.getTotalLength();
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
                bpm *= 1.5;
            } else if (bp.getMods().stream().anyMatch(r -> r.equals("HT"))) {
                length /= 0.75f;
                bpm *= 0.75f;
            }

            avgLength += length;

            if (Mod.hasChangeRating(bp.getScore())) {
                star += changedStarMap.get(bp.getBeatMap().getId()).getStars();
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

        sb.append("BP 平均长度: ").append(getTimeStr(avgLength)).append('\n');
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
        var mappersInfo = osuGetService.getUsers(mappersId).get("users");
        var mapperIdToInfo = new HashMap<Long, String>();
        for (var node : mappersInfo) {
            mapperIdToInfo.put(node.get("id").asLong(0), node.get("username").asText("unknown"));
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

        mapperData(float pp, int uid) {
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


