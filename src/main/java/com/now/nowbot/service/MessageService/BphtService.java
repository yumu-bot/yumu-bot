package com.now.nowbot.service.MessageService;

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
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.ServiceException.BindException;
import com.now.nowbot.util.QQMsgUtil;
import net.mamoe.mirai.message.data.At;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

@Service("bpht")
public class BphtService implements MessageService {
    private static final int FONT_SIZE = 30;

    private static final String TIPS = "请使用本功能的新设计面板 -> !bpa \n\n";
    OsuGetService osuGetService;
    BindDao       bindDao;
    ImageService  imageService;

    @Autowired
    public BphtService(OsuGetService osuGetService, BindDao bindDao, ImageService imageService) {
        this.osuGetService = osuGetService;
        this.bindDao = bindDao;
        this.imageService = imageService;
    }

    class intValue {
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
        BinUser nu = null;
        var at = QQMsgUtil.getType(event.getMessage(), AtMessage.class);
        // 是否为绑定用户
        boolean isBind = true;
        if (at != null) {
            nu = bindDao.getUser(at.getTarget());
        }
        if (matcher.group("name") != null && !matcher.group("name").trim().equals("")) {
            //查询其他人 bpht [name]
            String name = matcher.group("name").trim();
            long id = osuGetService.getOsuId(name);
            try {
                nu = bindDao.getUserFromOsuid(id);
            } catch (BindException e) {
                //do nothing
            }
            if (nu == null) {
                //构建只有 name + id 的对象
                nu = new BinUser();
                nu.setOsuID(id);
                nu.setOsuName(name);
                isBind = false;
            }
        } else {
            //处理没有参数的情况 查询自身
            nu = bindDao.getUser(event.getSender().getId());
        }
        //bp列表
        List<Score> Bps;
        //分别处理mode
        var mode = OsuMode.getMode(matcher.group("mode"));
        //处理默认mode
        if (mode == OsuMode.DEFAULT && nu.getMode() != null) mode = nu.getMode();
        Bps = osuGetService.getBestPerformance(nu, mode, 0, 100);

        String[] lins;
        if (matcher.group("info") == null) {
            if (mode == null || mode == OsuMode.DEFAULT) {
                lins = getAllMsg(Bps, nu.getOsuName(), "");
            } else {
                lins = getAllMsg(Bps, nu.getOsuName(), mode.getName());
            }
        } else {
            if (mode == null || mode == OsuMode.DEFAULT) {
                lins = getAllMsg(Bps, nu.getOsuName(), OsuMode.DEFAULT);
            } else {
                lins = getAllMsg(Bps, nu.getOsuName(), mode);
            }
        }
        QQMsgUtil.sendImage(from, imageService.drawLine(lins));
//        from.sendMessage(dtbf.toString());
    }

    public String[] getAllMsg(List<Score> Bps, String name, String mode) {
        var dtbf = new StringBuffer(TIPS)
                .append(name).append('[').append(mode).append(']').append('\n');
        double allPp = 0;
        int sSum = 0;
        int xSum = 0;
        int fcSum = 0;
        TreeMap<String, intValue> modeSum = new TreeMap<>(); //各个mod的数量

        DecimalFormat decimalFormat = new DecimalFormat("0.00"); //acc格式
        for (int i = 0; i < Bps.size(); i++) {
            var jsb = Bps.get(i);
            //显示前五跟后五的数据
            if (i < 5 || i > Bps.size() - 5) {
                dtbf.append("#")
                        .append(i + 1)
                        .append(' ')
                        .append(decimalFormat.format(jsb.getPP()))
                        .append(' ')
                        .append(decimalFormat.format(100 * jsb.getAccuracy()))
//                        .append(decimalFormat.format(accCoun.getAcc(jsb)))
                        .append('%')
                        .append(' ')
                        .append(jsb.getRank());
                if (jsb.getMods().size() > 0) {
                    for (int j = 0; j < jsb.getMods().size(); j++) {
                        dtbf.append(' ').append(jsb.getMods().get(j));
                    }
                }
                dtbf.append('\n');
            } else if (i == 50) {
                dtbf.append("-------分割线-------\n");
            }
            allPp += jsb.getPP(); //统计总数
            if (jsb.getMods().size() > 0) {
                for (int j = 0; j < jsb.getMods().size(); j++) {
                    String mod = jsb.getMods().get(j);
                    if (!modeSum.containsKey(mod)) modeSum.put(mod, new intValue());
                    else modeSum.get(mod).add();
                }
            }
            if (jsb.getRank().contains("S")) sSum++;
            if (jsb.getRank().contains("X")) {
                sSum++;
                xSum++;
            }
            if (jsb.isPerfect()) fcSum++;
        }
        dtbf.append("累计mod有:\n");
        modeSum.forEach((mod, sum) -> dtbf.append(mod).append(' ').append(sum.value).append(';'));
        dtbf.append("\n您bp中S rank及以上有").append(sSum).append("个\n达到满cb的fc有").append(fcSum).append('个');
        if (xSum != 0) dtbf.append("\n其中SS数量有").append(xSum).append('个');
        dtbf.append("\n您的BP1与BP100的差为").append(decimalFormat.format(Bps.get(0).getPP() - Bps.get(Bps.size() - 1).getPP()));
        dtbf.append("\n您的平均BP为").append(decimalFormat.format(allPp / Bps.size()));

        return dtbf.toString().split("\n");
    }

    public String[] getAllMsg(List<Score> bps, String name, OsuMode mode) {
        if (bps.size() == 0) return new String[0];
        var dtbf = new StringBuffer(TIPS)
                .append(name).append('[').append(mode).append(']').append('\n');

        var t1 = bps.get(0);
        var t1Bpm = t1.getBeatMap().getBpm();
        float t1BLength = t1.getBeatMap().getTotalLength();
        if (t1.getMods().contains("DT") || t1.getMods().contains("NC")) {
            t1BLength /= 1.5f;
            t1Bpm *= 1.5f;
        } else if (t1.getMods().stream().anyMatch(r -> r.equals("HT"))) {
            t1BLength /= 0.75f;
            t1Bpm *= 0.75f;
        }
        float star = 0;
        int maxBpm = 0;
        float maxBpmValue = t1Bpm;
        int maxCommbo = 0;
        int maxComboValue = t1.getMaxCombo();
        int maxLength = 0;
        float maxLengthValue = t1BLength;

        int minBpm = 0;
        float minBpmValue = maxBpmValue;
        int minCommbo = 0;
        int minComboValue = maxComboValue;
        int minLength = 0;
        float minLengthValue = maxLengthValue;

        int avgLength = 0;
        int avgCombo = 0;
        int maxTimeToPp = 0;
        float maxTimeToPpValue = 0;
        float allPP = 0;
        float nowPP = 0;

        TreeMap<String, modDate> modSum = new TreeMap<>(); //各个mod的数量

        TreeMap<Integer, mapperDate> mapperSum = new TreeMap<>();
        DecimalFormat decimalFormat = new DecimalFormat("0.00"); //acc格式

        var mapAttrGet = new MapAttrGet(mode);
        bps.stream()
                .peek(s -> {
                    if (s.getMods().size() == 0) s.setScore(0);
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
            var jsb = bps.get(i);
            var map = jsb.getBeatMap();
            int length = map.getTotalLength();
            float bpm = map.getBpm();
            jsb.getMods().forEach(r -> {
                if (modSum.containsKey(r)) {
                    modSum.get(r).add(jsb.getWeight().getPP());
                } else {
                    modSum.put(r, new modDate(jsb.getWeight().getPP()));
                }
            });
            if (jsb.getMods().contains("DT") || jsb.getMods().contains("NC")) {
                length /= 1.5f;
                bpm *= 1.5;
            } else if (jsb.getMods().stream().anyMatch(r -> r.equals("HT"))) {
                length /= 0.75f;
                bpm *= 0.75f;
            }

            avgLength += length;

            if (Mod.hasChangeRating(jsb.getScore())) {
                star += changedStarMap.get(jsb.getBeatMap().getId()).getStars();
            } else {
                star += jsb.getBeatMap().getDifficultyRating();
            }

            if (bpm < minBpmValue) {
                minBpm = i;
                minBpmValue = bpm;
            } else if (bpm > maxBpmValue) {
                maxBpm = i;
                maxBpmValue = bpm;
            }

            if (length < minLengthValue) {
                minLength = i;
                minLengthValue = length;
            } else if (length > maxLengthValue) {
                maxLength = i;
                maxLengthValue = length;
            }

            if (jsb.getMaxCombo() < minComboValue) {
                minCommbo = i;
                minComboValue = jsb.getMaxCombo();
            } else if (jsb.getMaxCombo() > maxComboValue) {
                maxCommbo = i;
                maxComboValue = jsb.getMaxCombo();
            }
            avgCombo += jsb.getMaxCombo();

            float tthToPp = (jsb.getPP()) / (map.getSliders() + map.getSpinners() + map.getCircles());
            if (maxTimeToPpValue < tthToPp) {
                maxTimeToPp = i;
                maxTimeToPpValue = tthToPp;
            }

            if (mapperSum.containsKey(map.getUserId())) {
                mapperSum.get(map.getUserId()).add(jsb.getPP());
            } else {
                mapperSum.put(map.getUserId(), new mapperDate(jsb.getPP(), map.getUserId()));
            }
            nowPP += jsb.getWeight().getPP();
            allPP += jsb.getPP();
        }
        avgCombo /= bps.size();
        avgLength /= bps.size();
        star /= bps.size();

        dtbf.append("bp平均长度: ").append(getTimeStr(avgLength)).append('\n');
        dtbf.append("最长是bp").append(maxLength + 1).append(' ').append(getTimeStr((int) maxLengthValue)).append('\n');
        dtbf.append("最短是bp").append(minLength + 1).append(' ').append(getTimeStr((int) minLengthValue)).append('\n');

        dtbf.append("bp平均combo: ").append(avgCombo).append('\n');
        dtbf.append("bp平均星级: ").append(star).append('\n');
        dtbf.append("最长是bp").append(maxCommbo + 1).append(' ').append(maxComboValue).append('\n');
        dtbf.append("最短是bp").append(minCommbo + 1).append(' ').append(minComboValue).append('\n');

        dtbf.append("单图pp/tth收益最大的是bp").append(maxTimeToPp + 1)
                .append(" 斩获").append(decimalFormat.format(maxTimeToPpValue)).append("pp/tth").append('\n');

        dtbf.append("bpm统计:").append(decimalFormat.format(maxBpmValue)).append('-').append(decimalFormat.format(minBpmValue)).append('\n');

        dtbf.append("bp中mapper统计:\n");
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

                dtbf.append(mapperIdToInfo.get(mapperDate.uid)).append(' ').append(mapperDate.size).append("个 总计")
                        .append(decimalFormat.format(mapperDate.allPP)).append("PP").append('\n');
            } catch (Exception e) {
                dtbf.append("id为").append(mapperDate.uid).append("暂未找到,但是有").append(mapperDate.size).append("个 总计")
                        .append(decimalFormat.format(mapperDate.allPP)).append("PP").append('\n');
            }
        });
        dtbf.append("累计mod有:\n");
        float finalAllPP = nowPP;
        modSum.forEach((mod, sum) -> dtbf.append(mod).append('*').append(sum.size).append(' ').append("总计")
                .append(sum.getAllPP())
                .append('[').append(decimalFormat.format(100 * sum.getAllPP() / finalAllPP)).append('%').append(']')
                .append('\n'));
        return dtbf.toString().split("\n");
    }

    class mapperDate {
        float allPP;
        int   size;
        long  uid;

        mapperDate(float pp, int uid) {
            allPP += pp;
            size = 1;
            this.uid = uid;
        }

        mapperDate add(float pp) {
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

    class modDate {
        float allPP;
        int   size;

        modDate(float pp) {
            allPP += pp;
            size = 1;
        }

        modDate add(float pp) {
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


