package com.now.nowbot.service.MessageService;

import com.now.nowbot.config.NowbotConfig;
import com.now.nowbot.dao.QQMessageDao;
import com.now.nowbot.throwable.TipsException;
import com.now.nowbot.util.ASyncMessageUtil;
import com.now.nowbot.util.SkiaUtil;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.Image;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.QuoteReply;
import net.mamoe.mirai.utils.ExternalResource;
import org.jetbrains.skija.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("cpanel")
/*
 我这是写了个啥,要不要删掉
 */
/*
 我趣，你为啥把猫bot的界面搬过来了
 */

public class CatpanelService implements MessageService {
    QQMessageDao qqMessageDao;
    @Autowired
    public CatpanelService(QQMessageDao qqMessageDao){
        this.qqMessageDao = qqMessageDao;
    }
    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {

        boolean stl = matcher.group("yl") != null;
        int an = matcher.group("bk") == null ? 0 : Integer.parseInt(matcher.group("bk"));
        if (an>100) an = 100;
        Image img;
        QuoteReply reply = event.getMessage().get(QuoteReply.Key);
        if (reply != null) {
            MessageChain msg = null;
            try {
                msg = qqMessageDao.getReply(reply);
            } catch (Exception e) {
                throw new TipsException("这张图不行!");
            }
            img = (Image) msg.stream().filter(it -> it instanceof Image).findFirst().orElse(
                    event.getMessage().stream().filter(it -> it instanceof Image).findFirst().orElse(null)
            );
        }else {
            img = (Image) event.getMessage().stream().filter(it -> it instanceof Image).findFirst().orElse(null);
        }

        if (img == null) {
            event.getSubject().sendMessage("没有任何图片");
            return;
        }
        var skijaimg = SkiaUtil.lodeNetWorkImage(Image.queryUrl(img));
        if (matcher.group("r") != null){
            qc(skijaimg, event);
            return;
        }
        var cutimg = SkiaUtil.getScaleCenterImage(skijaimg, 1200,857);

        var surface = Surface.makeRasterN32Premul(1200,857);
        var t1 = SkiaUtil.fileToImage(NowbotConfig.BG_PATH + "panel06.png");
        var t2 = SkiaUtil.fileToImage(NowbotConfig.BG_PATH + (stl?"ylbx.png":"lbx.png"));

        byte[] data;
        try(skijaimg;cutimg;surface;t1;t2){
            var canvas = surface.getCanvas();
            canvas.drawImage(cutimg,0,0);
            canvas.drawRect(Rect.makeWH(surface.getWidth(),surface.getHeight()),new Paint().setColor(Color.makeRGB(0,0,0)).setAlphaf(an/100f));
            canvas.drawImage(t1,0,0);
            canvas.drawImage(t2,0,0);
            data = surface.makeImageSnapshot().encodeToData(EncodedImageFormat.PNG).getBytes();
            event.getSubject().sendMessage(ExternalResource.uploadAsImage(ExternalResource.create(data),event.getSubject()));
        }
    }

    private void qc(org.jetbrains.skija.Image img, MessageEvent event) throws InterruptedException {
        var from = event.getSubject();
        int imgWidth = img.getWidth();
        int imgHeight = img.getHeight();
        var lock = ASyncMessageUtil.getLock(event);
        int w = 1200;
        int h = 857;
        int offsetX;
        int offsetY;
        Float scaleX;
        Float scaleY;
        if (1f * imgWidth / imgHeight < 1f * w/ h){
            scaleY = scaleX = 1f*w/imgWidth;
            offsetX = 0;
            offsetY = (int)(0.5f*(imgHeight*scaleX - h));
//            scaleX*"offsetY"
        }else {
            scaleY = scaleX = 1f*h/imgHeight;
            offsetX = (int)(0.5f*(imgWidth*scaleY - w)/scaleY);
            offsetY = 0;
        }
        Surface surface = Surface.makeRasterN32Premul(2*Math.round(scaleX*imgWidth), 2*Math.round(scaleY*imgWidth));
        try (surface){
            Canvas canvas = surface.getCanvas();
            StringBuffer sb = new StringBuffer();
            sb.append("接收到的图片分辨率为").append(imgWidth).append('x').append(imgHeight).append(",所需要的分辨率为1200x857").append('\n');
            sb.append("默认缩放倍率为").append(scaleX).append('x').append(scaleY).append("->").append(imgWidth*scaleX).append('x').append(imgHeight*scaleY).append('\n');
            sb.append("默认的裁切偏移(").append(offsetX).append(',').append(offsetY).append('\n').append("即将发送预览");
            from.sendMessage(sb.toString());
            canvas.drawImage(img,offsetX,offsetY);
            drowSee(canvas,offsetX,offsetY);
            from.sendMessage(
                    ExternalResource.uploadAsImage(
                            ExternalResource.create(
                                    surface.makeImageSnapshot(IRect.makeXYWH(0,0,(int)(imgWidth*scaleX),(int) (imgHeight*scaleY))).encodeToData(EncodedImageFormat.JPEG,40).getBytes()
                            ),from
                    )
            );
            //重缩放
            from.sendMessage("""
                    是否修改缩放大小,输入'倍率 倍率[:高度倍率]'修改范围[默认缩放值,10*默认缩放值] 或 '像素 宽度像素:高度像素'
                    输入倍率时,支持float精度,如果要分别修改高宽倍率,注意使用':'分隔,修改像素两个参数都要填
                    示例:倍率 1.25:1
                    输入像素时,输入宽/高不得低于1200:857,输入no跳过缩放设置,输入exit中止本次生成
                    示例:像素 1920:1080""");
            do {
                var nevent = ASyncMessageUtil.getEvent(lock);
                if (nevent == null) return;
                String page = nevent.getMessage().contentToString();
                if (page.contains("exit"))return;else
                if (page.contains("no"))break;else {
                    var p = Pattern.compile("(?<type>(像素)|(倍率)) (?<x>\\d+(\\.\\d+)?)(?![\\d\\.])(:(?<y>\\d+)(\\.\\d+)?)?");
                    var m = p.matcher(page);
                    if(m.find()){
                        if ("倍率".equals(m.group("type"))){
                            var temp = Float.parseFloat(m.group("x"));
                            if ((temp>=scaleX&&temp<=10*scaleX)){
                                scaleX = temp;
                            }else {
                                from.sendMessage("倍率范围无效");
                                continue;
                            }
                            if (m.group("y") == null){
                                scaleY = scaleX;
                            } else {
                                temp = Float.parseFloat(m.group("y"));
                                if ((temp>=scaleY&&temp<=10*scaleY)){
                                    scaleY = temp;
                                }else {
                                    from.sendMessage("倍率范围无效");
                                    continue;
                                }
                            }
                            from.sendMessage("倍率修改为 "+scaleX+'x'+scaleY+" -> 修改后像素"+imgWidth*scaleX+'x'+imgHeight*scaleY);
                            break;
                        }else {
                            var temp = Float.parseFloat(m.group("x"));
                            if (temp<1200 || m.group("y") == null || Float.parseFloat(m.group("y"))<857){
                                from.sendMessage("像素参数无效");
                                continue;
                            }
                            scaleX = temp / imgWidth;
                            scaleY = Float.parseFloat(m.group("y")) / imgHeight;
                            from.sendMessage("像素修改后 缩放倍率修改为 "+scaleX+'x'+scaleY+" -> 修改后像素"+imgWidth*scaleX+'x'+imgHeight*scaleY);
                            break;
                        }
                    }else {
                        from.sendMessage("参数错误,请重新尝试");
                        continue;
                    }
                }
            } while (true);
            canvas.clear(0);
            canvas.drawImage(img,offsetX,offsetY);
            drowSee(canvas,offsetX,offsetY);
            from.sendMessage(
                    ExternalResource.uploadAsImage(
                            ExternalResource.create(
                                    surface.makeImageSnapshot(IRect.makeXYWH(0,0,(int)(imgWidth*scaleX),(int) (imgHeight*scaleY))).encodeToData(EncodedImageFormat.JPEG,40).getBytes()
                            ),from
                    )
            );

        }
    }

    public static void main(String[] args) {
        var p = Pattern.compile("(?<type>(a)|(倍率)) (?<x>\\d+(\\.\\d+)?)(?![\\d\\.])(:(?<y>\\d+)(\\.\\d+)?)?");
        var m = p.matcher("倍率 12.63:4");
        while (m.find()){
            System.out.println(m.group("type"));
            System.out.println((int)Float.parseFloat(m.group("x")));
            System.out.println(m.group("y"));
        }
    }
    private void drowSee(Canvas canvas, int offsetX, int offsetY){
        canvas.save();
        canvas.translate(offsetX, offsetY);
        canvas.drawRect(Rect.makeWH(1200,857),new Paint().setARGB(100,230,0,0));
        canvas.restore();

    }
}
