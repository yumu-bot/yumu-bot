package com.now.nowbot.config;

import com.now.nowbot.listener.MessageListener;
import com.now.nowbot.throwable.RequestException;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.BotFactory;
import net.mamoe.mirai.utils.BotConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
@ConfigurationProperties(prefix = "nowbot.config")
public class NowbotConfig {
    private static final Logger log = LoggerFactory.getLogger(NowbotConfig.class);

    public static String RUN_PATH;
    @Value("${dir.rundir}")
    public void setRunPath(String RUN_PATH){
        Path pt = Path.of(RUN_PATH);
        if(!Files.isDirectory(pt)) {
            try {
                Files.createDirectories(pt);
            } catch (IOException e) {
                log.error(RUN_PATH+"创建失败",e);
            }
        }
        NowbotConfig.RUN_PATH = RUN_PATH;
    }

    public static String BOT_PATH;
    @Value("${dir.mirai}")
    public void setBotPath(String BOT_PATH){
        Path pt = Path.of(BOT_PATH);
        if(!Files.isDirectory(pt)) {
            try {
                Files.createDirectories(pt);
            } catch (IOException e) {
                log.error(BOT_PATH+"创建失败",e);
            }
        }
        NowbotConfig.BOT_PATH = BOT_PATH;
    }

    public static String BIN_PATH;
    @Value("${dir.bin}")
    public void setBinPath(String BIN_PATH) {
        Path pt = Path.of(BIN_PATH);
        if(!Files.isDirectory(pt)) {
            try {
                Files.createDirectories(pt);
            } catch (IOException e) {
                log.error(BIN_PATH+"创建失败",e);
            }
        }
        NowbotConfig.BIN_PATH = BIN_PATH;
    }

    public static String FONT_PATH;
    @Value("${dir.font}")
    public void setFontPath(String FONT_PATH){
        Path pt = Path.of(FONT_PATH);
        if(!Files.isDirectory(pt)) {
            try {
                Files.createDirectories(pt);
            } catch (IOException e) {
                log.error(FONT_PATH+"创建失败",e);
            }
        }
        NowbotConfig.FONT_PATH = FONT_PATH;
    }

    public static String BG_PATH;
    @Value("${dir.bgdir}")
    public void setBgPath(String BG_PATH){
        Path pt = Path.of(BG_PATH);
        if(!Files.isDirectory(pt)) {
            try {
                Files.createDirectories(pt);
            } catch (IOException e) {
                log.error(BG_PATH+"创建失败",e);
            }
        }
        NowbotConfig.BG_PATH = BG_PATH;
    }

    public static String IMGBUFFER_PATH;
    @Value("${dir.imghc}")
    public void setImgbufferPath(String IMGBUFFER_PATH){
        Path pt = Path.of(IMGBUFFER_PATH);
        if(!Files.isDirectory(pt)) {
            try {
                Files.createDirectories(pt);
            } catch (IOException e) {
                log.error(IMGBUFFER_PATH+"创建失败",e);
            }
        }
        NowbotConfig.IMGBUFFER_PATH = IMGBUFFER_PATH;
    }

    public static String OSU_ID;
    @Value("${dir.osuid}")
    public void setOsuId(String OSU_ID){
        Path pt = Path.of(OSU_ID);
        if(!Files.isDirectory(pt)) {
            try {
                Files.createDirectories(pt);
            } catch (IOException e) {
                log.error(OSU_ID+"创建失败",e);
            }
        }
        NowbotConfig.OSU_ID = OSU_ID;
    }

    public static long QQ;
    @Value("${mirai.qq}")
    public void setQQ(long QQ){
        NowbotConfig.QQ = QQ;
    }

    public static String PASSWORD;
    @Value("${mirai.password}")
    public void setPASSWORD(String PASSWORD){
        NowbotConfig.PASSWORD = PASSWORD;
    }

    @Bean
    public RestTemplate restTemplate() {
        var tempFactory = new OkHttp3ClientHttpRequestFactory();
        tempFactory.setConnectTimeout(3*60*1000);
        tempFactory.setReadTimeout(3*60*1000);
        var template = new RestTemplate(tempFactory);
        template.setErrorHandler(new DefaultResponseErrorHandler(){
            @Override
            public void handleError(ClientHttpResponse response, HttpStatus statusCode) throws RequestException {
                throw new RequestException(response, statusCode);
            }
        });
        return template;
    }
    @Value("${mirai.start}")
    boolean isLogin;
    public static boolean QQ_LOGIN;

    @Autowired
    MessageListener messageListener;
    @Bean
    public Bot bot(){
        //创建bot配置类
        BotConfiguration botConfiguration = new BotConfiguration();
        //设置配置
        botConfiguration.setCacheDir(new File(BOT_PATH));
        botConfiguration.setHeartbeatStrategy(BotConfiguration.HeartbeatStrategy.REGISTER);
        botConfiguration.setProtocol(BotConfiguration.MiraiProtocol.ANDROID_PHONE);
        botConfiguration.setWorkingDir(new File(BOT_PATH));

        File logdir = new File(BOT_PATH+"log");
        if (!logdir.isDirectory()) logdir.mkdirs();
        botConfiguration.redirectBotLogToDirectory(logdir);
        botConfiguration.redirectNetworkLogToDirectory(logdir);
        botConfiguration.fileBasedDeviceInfo();
        botConfiguration.enableContactCache();
        botConfiguration.getContactListCache().setSaveIntervalMillis(60000*30);
        //配置完成，注册bot
        Bot bot = BotFactory.INSTANCE.newBot(NowbotConfig.QQ,NowbotConfig.PASSWORD,botConfiguration);
        //登录
        QQ_LOGIN = isLogin;
        if (isLogin) bot.login();
        //注册监听 messageListener需要继承SimpleListenerHost类
//        bot.getEventChannel().registerListenerHost(messageListener);
        bot.getEventChannel().parentScope(messageListener).registerListenerHost(messageListener);
        return bot;
    }
    public static ApplicationContext applicationContext;
    @Autowired
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}