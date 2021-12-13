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
    private static int CATPANLE_WHITE = 1200;
    private static int CATPANLE_HEIGHT = 857;
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
            qc(skijaimg, event, CATPANLE_WHITE, CATPANLE_HEIGHT);
            return;
        }
        var cutimg = SkiaUtil.getScaleCenterImage(skijaimg, CATPANLE_WHITE,CATPANLE_HEIGHT);

        var surface = Surface.makeRasterN32Premul(CATPANLE_WHITE,CATPANLE_HEIGHT);
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

    private void qc(org.jetbrains.skija.Image img, MessageEvent event, int w, int h) throws InterruptedException, TipsException {
        var from = event.getSubject();
        int imgWidth = img.getWidth();
        int imgHeight = img.getHeight();
        var lock = ASyncMessageUtil.getLock(event);
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
            canvas.drawImage(img,0,0);
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
                    是否修改背景图大小,输入'倍率 倍率[:高度倍率]'修改范围[默认缩放值,10*默认缩放值] 或 '像素 宽度像素:高度像素'
                    输入倍率时,支持float精度,如果要分别修改高宽倍率,注意使用':'分隔,修改像素两个参数都要填
                    示例:倍率 1.25:1
                    输入像素时,输入宽/高不得低于1200:857,输入'确定'跳过或保存缩放设置,输入exit中止本次生成
                    示例:像素 1920:1080""");
            do {
                var nevent = ASyncMessageUtil.getEvent(lock);
                if (nevent == null) throw new TipsException("时间超时,停止本次任务");
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

                        }else {
                            var temp = Float.parseFloat(m.group("x"));
                            if (temp<CATPANLE_WHITE || m.group("y") == null || Float.parseFloat(m.group("y"))<CATPANLE_HEIGHT){
                                from.sendMessage("像素参数无效");
                                continue;
                            }
                            scaleX = temp / imgWidth;
                            scaleY = Float.parseFloat(m.group("y")) / imgHeight;
                        }


                        offsetX = (int)(0.5 * (scaleX * imgWidth - CATPANLE_WHITE));
                        offsetY = (int)(0.5 * (scaleY * imgWidth - CATPANLE_HEIGHT));

                        from.sendMessage("像素修改后 缩放倍率修改为 "+scaleX+'x'+scaleY+" -> 修改后像素"+imgWidth*scaleX+'x'+imgHeight*scaleY+"\n即将发送预览图");
                        canvas.clear(0);
                        canvas.save();
                        canvas.scale(scaleX, scaleY);
                        canvas.drawImage(img,0,0);
                        drowSee(canvas,(int)(offsetX/scaleX),(int)(offsetY/scaleY));
                        canvas.restore();
                        from.sendMessage(
                                ExternalResource.uploadAsImage(
                                        ExternalResource.create(
                                                surface.makeImageSnapshot(IRect.makeXYWH(0,0,(int)(imgWidth*scaleX),(int) (imgHeight*scaleY))).encodeToData(EncodedImageFormat.JPEG,40).getBytes()
                                        ),from
                                )
                        );
                        from.sendMessage("再次输入参数可以重新设定,输入'确定'保存缩放设置,输入exit中止本次生成");
                    }else {
                        from.sendMessage("参数错误,请重新尝试,输入'确定'保存缩放设置,输入exit结束");
                    }
                }
            } while (true);


//            from.sendMessage("""
//                    输入位置偏移""");
//            do {
//
//            }while (true);

//            from.sendMessage("""
//                    输入背景暗化程度""");
//            do {
//
//            }while (true);
            canvas.clear(0);
            canvas.save();
            canvas.scale(scaleX,scaleY);
            canvas.drawImage(img, -offsetX/scaleX, -offsetY/scaleY);
            from.sendMessage(
                    ExternalResource.uploadAsImage(
                            ExternalResource.create(
                                    surface.makeImageSnapshot(IRect.makeXYWH(0,0, CATPANLE_WHITE, CATPANLE_HEIGHT)).encodeToData(EncodedImageFormat.PNG).getBytes()
                            ),from
                    )
            );

        }
    }

    private void drowSee(Canvas canvas, int offsetX, int offsetY){
        canvas.save();
        canvas.translate(offsetX, offsetY);
        canvas.drawRect(Rect.makeWH(CATPANLE_WHITE,CATPANLE_HEIGHT),new Paint().setARGB(100,230,0,0));
        canvas.restore();

    }
}
