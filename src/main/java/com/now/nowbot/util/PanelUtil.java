package com.now.nowbot.util;

import com.now.nowbot.config.NowbotConfig;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.util.Panel.ACardBuilder;
import com.now.nowbot.util.Panel.BCardBuilder;
import com.now.nowbot.util.Panel.PPMPanelBuilder;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.skija.*;
import org.jetbrains.skija.svg.*;
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
    //设置抗锯齿
    static final Paint PAINT_ANTIALIAS = new Paint().setAntiAlias(true).setMode(PaintMode.FILL);
    //高斯模糊画笔
    static final Paint PAINT_BLUR = new Paint().setImageFilter(ImageFilter.makeBlur(10, 10, FilterTileMode.REPEAT));
    /* **
     * SS-D #FEF668 #F09450 #00B034 #3FBCEF #8E569B #EC6B76 #676EB0
     * 我方#00A8EC 对方 #FF0000
     */
    public static final int COLOR_X_PLUS = Color.makeRGB(254, 246, 104);
    public static final int COLOR_SS = Color.makeRGB(254, 246, 104);
    public static final int COLOR_S_PLUS = Color.makeRGB(240, 148, 80);
    public static final int COLOR_S = Color.makeRGB(240, 148, 80);
    public static final int COLOR_A_PLUS = Color.makeRGB(0, 176, 52);
    public static final int COLOR_A = Color.makeRGB(0, 176, 52);
    public static final int COLOR_B = Color.makeRGB(63, 188, 239);
    public static final int COLOR_C = Color.makeRGB(142, 86, 155);
    public static final int COLOR_D = Color.makeRGB(236, 107, 118);
    public static final int COLOR_F = Color.makeRGB(103, 110, 176);

    public static final int COLOR_HEX_ME = Color.makeRGB(0, 168, 236);
    public static final int COLOR_HEX_OTHER = Color.makeRGB(255, 0, 0);

    public static Image OBJECT_MAPSTATUS_RANKED;
    public static Image OBJECT_MAPSTATUS_QUALIFIED;
    public static Image OBJECT_MAPSTATUS_UNRANKED; //也是问号
    public static Image OBJECT_MAPSTATUS_LOVED;

    static {
        try {
            OBJECT_MAPSTATUS_RANKED = SkiaImageUtil.getImage(Path.of(NowbotConfig.BG_PATH, "ExportFileV3/object-beatmap-ranked.png"));
            OBJECT_MAPSTATUS_QUALIFIED = SkiaImageUtil.getImage(Path.of(NowbotConfig.BG_PATH, "ExportFileV3/object-beatmap-qualified.png"));
            OBJECT_MAPSTATUS_UNRANKED = SkiaImageUtil.getImage(Path.of(NowbotConfig.BG_PATH, "ExportFileV3/object-beatmap-unranked.png"));
            OBJECT_MAPSTATUS_LOVED = SkiaImageUtil.getImage(Path.of(NowbotConfig.BG_PATH, "ExportFileV3/object-beatmap-loved.png"));
        } catch (IOException e) {
//            e.printStackTrace();
        } catch (Exception e){}
    }

    public static Image OBJECT_CARD_SUPPORTER;

    public static int BANNER_INDEX_MIN = 1;
    public static int BANNER_INDEX_MAX = 50;

    //    
    public static final String MODE_OSU = "\uE800";
    public static final String MODE_CATCH = "\uE801";
    public static final String MODE_MANIA = "\uE802";
    public static final String MODE_TAIKO = "\uE803";

    private static Path PATH_FLAG;
    public static Path EXPORT_FOLE_V3;

    public static void init() {
        PATH_FLAG = Path.of(NowbotConfig.BG_PATH + "flag/");
        EXPORT_FOLE_V3 = Path.of(NowbotConfig.BG_PATH, "ExportFileV3");
        try {
//            OBJECT_MAPSTATUS_RANKED = SkiaImageUtil.getImage(NowbotConfig.BG_PATH + "ExportFileV3/object-mapstatus-Ranked.png");
//            OBJECT_MAPSTATUS_QUALIFIED = SkiaImageUtil.getImage(NowbotConfig.BG_PATH + "ExportFileV3/object-mapstatus-Qualified.png");
//            OBJECT_MAPSTATUS_UNRANKED = SkiaImageUtil.getImage(NowbotConfig.BG_PATH + "ExportFileV3/object-mapstatus-Unranked.png");
//            OBJECT_MAPSTATUS_UNKNOW = OBJECT_MAPSTATUS_UNRANKED;
//            OBJECT_MAPSTATUS_LOVED = SkiaImageUtil.getImage(NowbotConfig.BG_PATH + "ExportFileV3/object-mapstatus-Loved.png");
//
            OBJECT_CARD_SUPPORTER = SkiaImageUtil.getImage(Path.of(NowbotConfig.BG_PATH, "ExportFileV3/object-card-supporter.png"));
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
     *
     * @param code 国/区旗Code
     * @return svg
     * <p>
     * 旗子压缩成60*40的大小，位置横向 130 纵向 70
     */
    public static SVGDOM getFlag(String code) {
        if (!Files.isDirectory(PATH_FLAG)) {
            try {
                Files.createDirectories(PATH_FLAG);
            } catch (IOException e) {
                log.error("文件夹创建失败", e);
            }
        }

        if (code.length() >= 2) {
            code = code.substring(0, 2).toUpperCase();
            var flagFile = PATH_FLAG.resolve(code);
            SVGDOM svg;
            try {
                if (Files.isRegularFile(flagFile)) {
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

    public static String cutDecimalPoint(Double m) {
        if (m == null) return "";
        Double s = m - m.intValue();
        if (s < 0.01) return "";
        String r = s.toString();
        return r.substring(1, Math.min(r.length(), 4));
    }

    /**
     * 获得个人背景,如没有则默认从url获取
     *
     * @param isBlur 是否模糊暗化
     */
    public static Image getBgUrl(@Nullable String filePath, String url, boolean isBlur) {
        Image img = null;
        try {
            if (filePath != null && Files.isRegularFile(Path.of(filePath))) {
                img = SkiaImageUtil.getImage(filePath);
                return img;
            }
            img = SkiaImageUtil.getImage(url);
        } catch (IOException e) {
            log.error("文件读取异常", e);
        }
        if (isBlur)
            return getBlur(img);
        else return img;
    }

    /**
     * 获得个人背景,如没有则默认从文件路径获取
     *
     * @param isBlur 是否模糊暗化
     */
    public static Image getBgFile(@Nullable String filePath, String path, boolean isBlur) {
        Image img;
        if (filePath != null && Files.isRegularFile(Path.of(filePath))) {
            try {
                img = SkiaImageUtil.getImage(filePath);
                return img;
            } catch (IOException e) {
                log.error("文件读取异常", e);
            }
        }
        try {
            img = SkiaImageUtil.getImage(path);
            if (isBlur)
                return getBlur(img);
            else return img;
        } catch (IOException e) {
            log.error("默认图片加载异常", e);
            throw new RuntimeException("图片加载异常");
        }
    }

    protected static Image getBlur(Image img) {
        try (Surface s = Surface.makeRasterN32Premul(img.getWidth(), img.getHeight())) {
            s.getCanvas().drawImage(img, 0, 0, PAINT_BLUR);
            s.getCanvas().drawRect(Rect.makeWH(s.getWidth(), s.getHeight()), new Paint().setAlphaf(0.4f));
            img = s.makeImageSnapshot();
        }
        return img;
    }

    static int bannerIndex = 1;

    public static Image getBanner(BinUser user) throws IOException {
        if (bannerIndex > BANNER_INDEX_MAX) {
            bannerIndex = BANNER_INDEX_MIN;
        }
        Image banner = SkiaImageUtil.getImage(EXPORT_FOLE_V3.resolve("Banner/b" + bannerIndex + ".png").toString());
        bannerIndex++;
        return banner;
    }

    public static void main(String[] args) throws IOException {
        System.out.println(1);
        var svgdom = new SVGDOM(Data.makeFromBytes(Files.readAllBytes(Path.of("/home/spring/code/nowbot-img/templete/1.svg"))));
        var s = Surface.makeRasterN32Premul(600,600);
        var c = s.getCanvas();
        svgdom.getRoot()
                .setWidth(new SVGLength(600))
                .setPreserveAspectRatio(new SVGPreserveAspectRatio(SVGPreserveAspectRatioAlign.XMIN_YMIN, SVGPreserveAspectRatioScale.SLICE));
        System.out.println(2);
        svgdom.render(c);

        Files
                .write(Path.of("/home/spring/code/nowbot-img/templete/1.png"), s.makeImageSnapshot().encodeToData().getBytes());
        System.out.println(3);
    }
}