package com.now.nowbot.util.Panel;

import com.now.nowbot.model.JsonData.BpInfo;
import com.now.nowbot.util.SkiaUtil;
import org.jetbrains.skija.*;

import java.text.DecimalFormat;
import java.util.List;
import java.util.TreeMap;

public class BphtPanelBuilder{
    private static final int FONT_SIZE = 30;
    private Font font;
    Image image;
    String[] assess = {
            "这...还要多练",
            "水平有点菜,有待进步",
            "不错不错,可以多打点pp图",
            "多刷点flow图吧",
            "不好评价",
            "你这bp,别刷跳跳跳了",
            "兄弟,耐力不行吧,多练",
            "合理怀疑开挂",
            "能不能打打长图",
            "别查了,快去刷pp!",
            "不好评价",
            "不好评价",
    };

    public BphtPanelBuilder() {
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
    public BphtPanelBuilder draw(List<BpInfo> Bps, String name, String mode){

        var dtbf = new StringBuffer(name).append('[').append(mode).append(']').append('\n');
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
                        .append(decimalFormat.format(jsb.getPp()))
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
            allPp += jsb.getPp(); //统计总数
            if (jsb.getMods().size() > 0) {
                for (int j = 0; j < jsb.getMods().size(); j++) {
                    String mod = jsb.getMods().get(j);
                    if (modeSum.get(mod) == null) modeSum.put(mod, new intValue());
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
        dtbf.append("\n您的BP1与BP100的差为").append(decimalFormat.format(Bps.get(0).getPp() - Bps.get(Bps.size() - 1).getPp()));
        dtbf.append("\n您的平均BP为").append(decimalFormat.format(allPp / Bps.size()));
        if (Bps.size()>30) {
            var tr = Bps.get(2).getPp().intValue()+(int) (Bps.get(9).getPp()*Bps.get(7).getAccuracy());
            dtbf.append('\n').append(assess[tr%assess.length]);
        }else {
            dtbf.append('\n').append("不好评价");
        }

        var allstr = dtbf.toString().split("\n");
        TextLine[] lines = new TextLine[allstr.length];
        float maxWidth = 0;
        for (int i = 0; i < allstr.length; i++) {
            lines[i] = TextLine.make(allstr[i], getFont());
            if (maxWidth < lines[i].getWidth()) {
                maxWidth = lines[i].getWidth();
            }
        }
        int w = (int) maxWidth + 50;
        int h = (int) ((lines.length + 1) * lines[0].getHeight()) + 50;

        Surface surface = Surface.makeRasterN32Premul(w, h);
        Shader shader = Shader.makeLinearGradient(0, 0, 0, h, SkiaUtil.getRandomColors());
        try (surface; shader) {
            var canvas = surface.getCanvas();
            canvas.clear(Color.makeRGB(38, 51, 57));
//            canvas.drawRect(Rect.makeWH(w,h),new Paint().setShader(shader));
            canvas.translate(25, 40);
            for (TextLine line : lines) {
                canvas.drawTextLine(line, 0, line.getCapHeight() + FONT_SIZE * 0.2f, new Paint().setColor(SkiaUtil.getRandomColor()));
                canvas.translate(0, line.getHeight());
            }
            image = surface.makeImageSnapshot();
        } finally {
            for (var line : lines) {
                line.close();
            }
        }
        return this;
    }

    public Image build() {
        //这里是自定义输出
        return image;
    }

    private Font getFont() {
        if (font == null) {
            font = new Font(SkiaUtil.getPUHUITI(), FONT_SIZE);
        }
        return font;
    }
}
class test extends PanelBuilder{
    /**
     * 知道确定大小的
     */
    test(){
        //创建固定大小的面板 初始化来自父类的面板,就是之后的 surface跟canvas 不需要自己弄
        super(500, 300);
        System.out.println(width);
        System.out.println(hight);
    }

    public test testdraw(){
        canvas.save();
        var text = "测试文字";
        var typeface = Typeface.makeDefault();
        var size = 50;//字号大小
        var textimage = SkiaUtil.getTextImage(text, typeface, size, new Paint().setColor(Color.makeRGB(38, 51, 57)));
        //这里的width hight 是画板大小
        canvas.translate((width-textimage.getWidth())/2f, (hight - textimage.getHeight())/2f);
        canvas.drawImage(textimage,0,0);
        canvas.restore();
        return this;
    }

    public Image build(){
        //输出,这里调用的父类中剪裁圆角的输出,第一个参数数字是输出圆角半径,第二个是右上角的小字信息
        return build(20,"bpht");
    }
}