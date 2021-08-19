package com.now.nowbot.service.msgServiceImpl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.now.nowbot.config.NowbotConfig;
import com.now.nowbot.entity.BinUser;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.util.BindingUtil;
import com.now.nowbot.util.SkiaUtil;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.utils.ExternalResource;
import org.jetbrains.skija.*;
import org.jetbrains.skija.paragraph.*;
import org.jetbrains.skija.svg.SVGDOM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.web.client.HttpClientErrorException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class FriendServiceImpl extends MessageService{
    private final static Logger log = LoggerFactory.getLogger(FriendServiceImpl.class);
    @Autowired
    OsuGetService osuGetService;

    public static Typeface face;
    static Font naf;
    static Font bigf;
    public FriendServiceImpl(){
        super("friend");
    }

    @Override
    public void handleMsg(MessageEvent event) {
        Contact from;
        if(event instanceof GroupMessageEvent) {
            from = ((GroupMessageEvent) event).getGroup();
        }else {
            from = event.getSender();
        }
        if(face == null){
            face = Typeface.makeFromFile(NowbotConfig.FONT_PATH +"font.otf");
            naf = new Font(face, 25);
            bigf = new Font(face, 50);
        }
        BinUser user = BindingUtil.readUser(event.getSender().getId());
        if (user == null) {
            from.sendMessage(new At(event.getSender().getId()).plus("您未绑定，请发送bind绑定"));
            return;
        }
        JSONArray friendList = null;
        try {
            friendList = osuGetService.getFrendList(user);
        } catch (HttpClientErrorException e) {
            from.sendMessage("您的令牌已过期,请私聊bot bind重新绑定！");
        }

        int start = 0;
        int end = 24;
        Pattern p = Pattern.compile("(?<fr>\\d+)(-(?<sc>\\d+))?");
        Matcher m = p.matcher(event.getMessage().contentToString());
        if (m.find()) {
            if (m.group("sc") == null) {
                end = Integer.parseInt(m.group("fr"));
                if (end >= friendList.size()) {
                    end = friendList.size();
                }
                if(end>100) end = 100;
                from.sendMessage("正在处理好友:1-" + end+"\n您全部好友有"+friendList.size()+"个");
            } else {
                start = Integer.parseInt(m.group("fr"))-1;
                end = Integer.parseInt(m.group("sc"));
                if ((start < 0)) {
                    start = 0;
                }
                if (end >= friendList.size()) {
                    end = friendList.size();
                }
                if((end - start) > 100){
                    end = start+100;
                }
                from.sendMessage("正在处理好友:" + (start+1) + "-" + (end)+"\n您全部好友有"+friendList.size()+"个");
            }

        }

        else from.sendMessage("正在处理，因为加载慢，所以默认加载前1-24个，可以使用[x-y]或[n]指定长度,最大处理100个\n您全部好友有"+friendList.size()+"个");


        List<JSONObject> jsons = friendList.toJavaList(JSONObject.class);
        byte[] pngBytes = null;
        CopyOnWriteArrayList<Image> images = new CopyOnWriteArrayList<>();
        List<Future<Image>> futureList = new LinkedList<>();
        for(int i = start; i < end && i < jsons.size(); i++){
                    Future<Image> result = getImage(jsons.get(i).getString("username"),
                    jsons.get(i).getString("avatar_url"),
                    jsons.get(i).getJSONObject("cover").getString("url"),
                    jsons.get(i).getJSONObject("country").getString("code"),
                    jsons.get(i).getJSONObject("statistics").getString("pp")
                    );
            futureList.add(result);
        }
        futureList.forEach(future -> {
            try {
                images.add(future.get());
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        });
        try(Surface surface = Surface.makeRasterN32Premul(1000,100 + (images.size()/3+1)*130)){
            var canvas = surface.getCanvas();
            canvas.clear(Color.makeRGB(62,57,70));
            TextLine text = TextLine.make(user.getOsuName(),bigf);
            canvas.drawTextLine(text,(1000-text.getWidth())/2, text.getHeight(),new Paint().setARGB(255,255,255,255));


            for (int i = 0; i < images.size(); i++) {
                canvas.drawImage(images.get(i),55+(i%3)*300,100+(i/3)*130);
            }
            pngBytes = surface.makeImageSnapshot().encodeToData().getBytes();
        }


        if (pngBytes != null){
            from.sendMessage(
                    ExternalResource.uploadAsImage(
                            ExternalResource.create(pngBytes), from
                    ));
        }

        futureList.removeIf(f -> f.cancel(true) | true);
        System.gc();
        return;
    }

    public static String getFlagUrl(String ct){
        int A =  0x1f1e6;
        char x1 = ct.charAt(0);
        char x2 = ct.charAt(1);
        int s1 = A + x1-'A';
        int s2 = A + x2-'A';
        return "https://osu.ppy.sh/assets/images/flags/"+Integer.toHexString(s1)+"-"+Integer.toHexString(s2)+".svg";
    }

    @Async
    Future<Image> getImage(String name, String headUrl, String bgUrl, String ct, String pp) {
        Image image = null;
        try(Surface pa = Surface.makeRasterN32Premul(290,120)) {
            var canvas = pa.getCanvas();
            Image head = SkiaUtil.lodeNetWorkImage(headUrl);
            Image bg = SkiaUtil.lodeNetWorkImage(bgUrl);

            int imgWidth = bg.getWidth();
            int imgHeight = bg.getHeight();


            if (1f * imgWidth / imgHeight < 290f / 210) {
                bg = SkiaUtil.getScaleImage(bg, 290, 290 * imgHeight / imgWidth);
            } else {
                bg = SkiaUtil.getScaleImage(bg, 210 * imgWidth / imgHeight, 210);
            }
            SkiaUtil.drowCutRRectImage(canvas, bg, 0, 0, (bg.getWidth() - 290) / 2, 0, 290, 120, 10,new Paint().setImageFilter(ImageFilter.makeBlur(10,10,FilterTileMode.REPEAT)));
            canvas.drawRRect(RRect.makeXYWH(0,0,290,120,10),new Paint().setARGB(80,0,0,0));
            head = SkiaUtil.getScaleImage(head, 100, 100);
            SkiaUtil.drowRRectImage(canvas, head, 10, 10, 10);
            Path flagFile = Path.of(NowbotConfig.RUN_PATH+"flag/"+ct+".svg");
            byte[] svgbytes;
            try {
                if(Files.isRegularFile(flagFile)){
                    svgbytes = Files.readAllBytes(flagFile);
                }else {
                    URL url = new URL(getFlagUrl(ct));
                    HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
                    httpConn.connect();
                    InputStream cin = httpConn.getInputStream();
                    svgbytes = cin.readAllBytes();
                    cin.close();
                    Files.createFile(flagFile);
                    Files.write(flagFile,svgbytes);
                }
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
            try (SVGDOM flag = new SVGDOM(Data.makeFromBytes(svgbytes))){
                SkiaUtil.drowSvg(canvas, flag, 120, 2, 60, 60);
            }
            canvas.drawString("PP:"+pp.split("\\.")[0], 185, 40,naf, new Paint().setARGB(255,255,255,255));
            var ts = new TextStyle().setTypeface(face).setFontStyle(FontStyle.NORMAL).setLetterSpacing(-2f).setColor(0xffffffff).setFontSize(25);
            try (ParagraphStyle ps   = new ParagraphStyle();
                 ParagraphBuilder pb = new ParagraphBuilder(ps, new FontCollection().setDefaultFontManager(FontMgr.getDefault()));)
            {
                pb.pushStyle(ts);
                pb.addText(name);
                try (Paragraph p = pb.build();) {
                    p.layout(Float.POSITIVE_INFINITY);
                    p.paint(canvas, 120, 60);
                }
            }

            image = pa.makeImageSnapshot();
        }
        return new AsyncResult<>(image);
    }
}
