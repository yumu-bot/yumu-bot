package com.now.nowbot.util.Panel;

import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.util.SkiaImageUtil;
import com.now.nowbot.util.SkiaUtil;
import org.jetbrains.skija.*;

import java.io.IOException;
import java.util.ArrayList;

public class J1CardBuilder extends PanelBuilder {

    public static final ArrayList<Float> F48L = new ArrayList<>();

    public J1CardBuilder(OsuUser user, OsuMode mode) {
        super(430, 355);

        drawBaseRRect();
        drawUserCover(user);
        drawUserAvatar(user);
        drawCardText(mode);
        drawUserText(user, mode);
    }

    private void drawBaseRRect() {
        canvas.clear(Color.makeRGB(56, 46, 50));
    }

    private void drawUserCover(OsuUser user) {
        //画头像后的背景层
        canvas.save();
        canvas.translate(20, 20);
        canvas.drawRRect(RRect.makeXYWH(0, 0, 390, 210, 20), new Paint().setARGB(255, 56, 46, 50));
        canvas.clipRRect(RRect.makeXYWH(0, 0, 390, 210, 20));
        Image J1UserCover = null;
        try {
            J1UserCover = SkiaImageUtil.getImage(user.getCoverUrl());
        } catch (IOException e) {
            throw new RuntimeException(" get cover image error ");
        }
        Image J1CardBGSC = SkiaImageUtil.getScaleCenterImage(J1UserCover, 420, 240); //缩放至合适大小，这里放大了一点，以应对模糊带来的负面效果
        canvas.drawImage(J1CardBGSC, 0, 0, new Paint().setAlphaf(0.2f).setImageFilter(ImageFilter.makeBlur(5, 5, FilterTileMode.REPEAT)));
        canvas.restore();
    }

    private void drawUserAvatar(OsuUser user) {
        //画头像
        canvas.save();
        canvas.translate(165, 35);
        canvas.drawRRect(RRect.makeXYWH(0, 0, 100, 100, 10), new Paint().setARGB(255, 56, 46, 50));
        canvas.clipRRect(RRect.makeXYWH(0, 0, 100, 100, 10));
        Image J1UserAvatar = null;
        try {
            J1UserAvatar = SkiaImageUtil.getImage(user.getAvatarUrl());
        } catch (IOException e) {
            throw new RuntimeException(" get head image error ");
        }
        Image J1CardBGSC = SkiaImageUtil.getScaleCenterImage(J1UserAvatar, 100, 100); //缩放至合适大小，
        canvas.drawImage(J1CardBGSC, 0, 0, new Paint().setAlphaf(1f));
        canvas.restore();
    }

    private void drawCardText(OsuMode mode) {
        //画卡片基础信息
        Typeface TorusSB = SkiaUtil.getTorusSemiBold();
        Font fontS24 = new Font(TorusSB, 24);

        String J1t = "0";
        String J2t = "0";
        String J3t = "0";

        if (mode == OsuMode.MANIA) {
            J1t = "4K.PP";
            J2t = "7K.PP";
            J3t = "M.Combo";
        } else {
            J1t = "PP";
            J2t = "raw.PP";
            J3t = "M.Combo";
        }

        TextLine J1 = TextLine.make(J1t, fontS24);
        TextLine J2 = TextLine.make(J2t, fontS24);
        TextLine J3 = TextLine.make(J3t, fontS24);

        canvas.save();
        canvas.translate(80 - J1.getWidth() / 2, 300);//居中处理
        canvas.drawTextLine(J1, 0, J1.getHeight() - J1.getXHeight(), new Paint().setARGB(255, 170, 170, 170));
        canvas.translate(135 + (J1.getWidth() - J2.getWidth()) / 2, 0);//居中处理
        canvas.drawTextLine(J2, 0, J2.getHeight() - J2.getXHeight(), new Paint().setARGB(255, 170, 170, 170));
        canvas.translate(135 + (J2.getWidth() - J3.getWidth()) / 2, 0);//居中处理
        canvas.drawTextLine(J3, 0, J3.getHeight() - J3.getXHeight(), new Paint().setARGB(255, 170, 170, 170));
        canvas.restore();
    }

    private void drawUserText(OsuUser user, OsuMode mode) {
        //画用户基础信息
        Typeface TorusSB = SkiaUtil.getTorusSemiBold();
        Font fontS48 = new Font(TorusSB, 48);
        Font fontS36 = new Font(TorusSB, 36);
        Font fontS24 = new Font(TorusSB, 24);

        canvas.save();
        String J1t = "0";
        String J2t = "0";
        String J3t = "0";
        String J4t = "Anonymous";
        String J5t = "0";

        double rawPP = user.getPp() - (1000 / 2.4 * (1 - Math.pow(0.9994, user.getPlayCount()))); //416.6667 // PlayCount -> user.getBeatmapPlaycount()

        if (mode == OsuMode.MANIA) {
            J1t = String.valueOf(Math.round(user.getStatustucs().getPP4K()));
            J2t = String.valueOf(Math.round(user.getStatustucs().getPP7K()));
            J3t = String.valueOf(user.getMaxCombo());
        } else {
            J1t = String.valueOf(Math.round(user.getPp()));
            J2t = String.valueOf(Math.round(rawPP));
            J3t = String.valueOf(user.getMaxCombo());
        }

        J5t = String.valueOf(user.getId());

        TextLine J1 = TextLine.make(J1t, fontS36);
        TextLine J2 = TextLine.make(J2t, fontS36);
        TextLine J3 = TextLine.make(J3t, fontS36);
        // T4 需要缩进
        TextLine J5 = TextLine.make(J5t, fontS24);

        // T4 缩进方法 copied from HCard
        StringBuilder sb = new StringBuilder();
        float allWidth = 0;
        int backL = 0;
        int maxWidth = 350; // 最大宽度
        float pointW48 = 3 * F48L.get('.');

        var NameChar = user.getUsername().toCharArray(); // J4t = user.getUsername();
        //计算字符长度
        for (var thisChar : NameChar) {
            if (allWidth > maxWidth) {
                break;
            }
            sb.append(thisChar);
            allWidth += F48L.get(thisChar);
            if ((allWidth + pointW48) < maxWidth) {
                backL++;
            }
        }
        if (allWidth > maxWidth) {
            sb.delete(backL, sb.length());
            sb.append("...");
        }
        TextLine J4 = TextLine.make(sb.toString(), fontS48);
        sb.delete(0, sb.length());

        canvas.save();
        canvas.translate(80 - J1.getWidth() / 2, 255);//居中处理
        canvas.drawTextLine(J1, 0, J1.getHeight() - J1.getXHeight(), new Paint().setARGB(255, 255, 255, 255));
        canvas.translate(135 + (J1.getWidth() - J2.getWidth()) / 2, 0);//居中处理
        canvas.drawTextLine(J2, 0, J2.getHeight() - J2.getXHeight(), new Paint().setARGB(255, 255, 255, 255));
        canvas.translate(135 + (J2.getWidth() - J3.getWidth()) / 2, 0);//居中处理
        canvas.drawTextLine(J3, 0, J3.getHeight() - J3.getXHeight(), new Paint().setARGB(255, 255, 255, 255));
        canvas.restore();

        canvas.save();
        canvas.translate(215 - J4.getWidth() / 2, 150);//居中处理
        canvas.drawTextLine(J4, 0, J4.getHeight() - J4.getXHeight(), new Paint().setARGB(255, 255, 255, 255));
        canvas.translate((J4.getWidth() - J5.getWidth()) / 2, 50);//居中处理
        canvas.drawTextLine(J5, 0, J5.getHeight() - J5.getXHeight(), new Paint().setARGB(255, 255, 255, 255));
        canvas.restore();
    }

    public Image build() {
        return super.build(20);
    }
}
