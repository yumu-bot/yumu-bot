package com.now.nowbot.service.MessageService;

import com.now.nowbot.aop.CheckPermission;
import com.now.nowbot.config.NowbotConfig;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.util.BindingUtil;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.Image;
import net.mamoe.mirai.utils.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;

@Service("setu")
public class SetuService implements MessageService{
    static final Logger log = LoggerFactory.getLogger(SetuService.class);
    Long time = 0L;
    @Autowired
    RestTemplate template;


    @Override
    @CheckPermission(friend = false)
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        Contact from = event.getSubject();
        long qq = event.getSender().getId();
        if (qq > -9){
            return;
        }
        synchronized (time){
            if(time+(15*1000)>System.currentTimeMillis()){
                byte[] img = new byte[0];
                try {
                    img = Files.readAllBytes(Path.of(NowbotConfig.BG_PATH+"xxoo.jpg"));
                } catch (IOException e) {
                    log.error("图片读取异常",e);
                }
                from.sendMessage(ExternalResource.uploadAsImage(ExternalResource.create(img),from));
                return;
            }else
                time = System.currentTimeMillis();
        }
        BinUser binUser = BindingUtil.readUser(qq);

        if(true){
//            SendmsgUtil.send(from,"稍等片刻");
            from.sendMessage("稍等片刻");
        }else {
//            from.sendMessage("您当前所剩积分："+score.getStar()+'\n'+"不足5积分,无法看图！");
//            return;
        }



        Image img = null;
        try {
            byte[] date;
            int random = Math.toIntExact((System.currentTimeMillis() % 5));
            switch (random){
                case 3:{
                    date = api2();
                }break;
                case 1:{
                    date = api3();
                }break;
                default:{
                    date = api1();
                }
            }
            img = from.uploadImage(ExternalResource.create(date));
        } catch (IOException e) {
            e.printStackTrace();
            from.sendMessage("api异常，请稍后再试，积分已退回");
        }
        if (img != null) {
            from.sendMessage(img).recallIn(110*1000);
        }
    }
    public byte[] api3()throws Exception{
        //todo 检查下载失败原因
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
        HttpEntity<Byte[]> httpEntity = new HttpEntity<>(headers);
        ResponseEntity<byte[]> date = null;
        date = template.exchange("https://www.dmoe.cc/random.php", HttpMethod.GET, httpEntity, byte[].class);
        return date.getBody();
    }
    public byte[] api2() throws Exception{
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
        HttpEntity<Byte[]> httpEntity = new HttpEntity<>(headers);
        ResponseEntity<byte[]> date = null;
        date = template.exchange("https://api.vvhan.com/api/girl", HttpMethod.GET, httpEntity, byte[].class);
        return date.getBody();
    }
    public byte[] api1() throws Exception{
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
        HttpEntity<Byte[]> httpEntity = new HttpEntity<>(headers);
        ResponseEntity<byte[]> date = null;
        date = template.exchange("https://iw233.cn/API/GHS.PHP", HttpMethod.GET, httpEntity, byte[].class);
        return date.getBody();
    }
}
