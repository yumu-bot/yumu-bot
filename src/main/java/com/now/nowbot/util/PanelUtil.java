package com.now.nowbot.util;

import com.now.nowbot.config.NowbotConfig;
import com.now.nowbot.util.Panel.ACardBuilder;
import com.now.nowbot.util.Panel.BCardBuilder;
import com.now.nowbot.util.Panel.PPMPanelBuilder;
import org.jetbrains.skija.Color;
import org.jetbrains.skija.Data;
import org.jetbrains.skija.Image;
import org.jetbrains.skija.svg.SVGDOM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

public class PanelUtil {
    static final Logger log = LoggerFactory.getLogger(PanelUtil.class);
    /* **
     * SS-D #FEF668 #F09450 #00B034 #3FBCEF #8E569B #EC6B76 #676EB0
     * 我方#00A8EC 对方 #FF0000
     */
    public static final int COLOR_SS = Color.makeRGB(254, 246, 104);
    public static final int COLOR_S = Color.makeRGB(240, 148, 80);
    public static final int COLOR_A_PLUS = Color.makeRGB(0, 176, 52);
    public static final int COLOR_A = Color.makeRGB(0, 176, 52);
    public static final int COLOR_B = Color.makeRGB(63, 188, 239);
    public static final int COLOR_C = Color.makeRGB(142, 86, 155);
    public static final int COLOR_D = Color.makeRGB(236,107,118);
    public static final int COLOR_F = Color.makeRGB(103,110,176);

    public static final int COLOR_HEX_ME = Color.makeRGB(0, 168, 236);
    public static final int COLOR_HEX_OTHER = Color.makeRGB(255, 0, 0);

    public static Image OBJECT_MAPSTATUS_QUALIFIED;

    private static Path PATH_FLAG;
    public static void init(){
        /* **
         * 爱心loved 对勾approved和qualified 蓝箭头ranked 其他均为第四个问号
         * 图片左上角在横向 370 纵向 10 的位置
         */
        PATH_FLAG = Path.of(NowbotConfig.BG_PATH+"flag/");
        try {
            OBJECT_MAPSTATUS_QUALIFIED = SkiaUtil.fileToImage(NowbotConfig.BG_PATH+"object-mapstatus-Qualified.png");
            OBJECT_MAPSTATUS_QUALIFIED = SkiaUtil.fileToImage(NowbotConfig.BG_PATH+"object-mapstatus-Ranked.png");
            OBJECT_MAPSTATUS_QUALIFIED = SkiaUtil.fileToImage(NowbotConfig.BG_PATH+"object-mapstatus-Unranked.png");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /***
     * 玩家卡片
     * @return
     */
    public static ACardBuilder getA1Builder(Image bg) {
        return new ACardBuilder(bg);
    }

    /***
     * 谱面B成绩卡片
     * @return
     */
    public static BCardBuilder getA2Builder(Image bg) {
        return new BCardBuilder(bg);
    }

    /***
     * PPA(P)面板
     * @return
     */
    public static PPMPanelBuilder getPPMBulider() {
        return new PPMPanelBuilder();
    }

    /**
     * 获得国/区旗的SVG
     * @param code 国/区旗Code
     * @return svg
     *
     * 旗子压缩成60*40的大小，位置横向 130 纵向 70
     */
    public static SVGDOM getFlag(String code){
        if (!Files.isDirectory(PATH_FLAG)) {
            try {
                Files.createDirectories(PATH_FLAG);
            } catch (IOException e) {
                log.error("文件夹创建失败", e);
            }
        }

        if (code.length() >= 2) {
            code = code.substring(0,2).toUpperCase();
            var flagFile = PATH_FLAG.resolve(code);
            SVGDOM svg;
            try {
                if (Files.isRegularFile(flagFile)){
                    var svgbytes = Files.readAllBytes(flagFile);
                    svg = new SVGDOM(Data.makeFromBytes(svgbytes));
                } else {
                    int A = 0x1f1e6;
                    char x1 = code.charAt(0);
                    char x2 = code.charAt(1);
                    int s1 = A + x1 - 'A';
                    int s2 = A + x2 - 'A';
                    var path = "https://osu.ppy.sh/assets/images/flags/" + Integer.toHexString(s1) + "-" + Integer.toHexString(s2) + ".svg";
                    URL url = new URL(path);
                    HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
                    httpConn.connect();
                    InputStream cin = httpConn.getInputStream();
                    byte[] svgbytes = cin.readAllBytes();
                    cin.close();
                    Files.write(flagFile, svgbytes);
                    svg = new SVGDOM(Data.makeFromBytes(svgbytes));
                }
            } catch (IOException e) {
                log.error("国/区旗加载异常", e);
                return null;
            }
            return svg;
        }
        log.error("国/区旗参数长度不足");
        return null;
    }

    public static String cutDecimalPoint(Double m){
        if (m == null) return "";
        Double s = m - m.intValue();
        if (s < 0.01) return "";
        String r = s.toString();
        return r.substring(1,Math.min(r.length(),4));
    }
}