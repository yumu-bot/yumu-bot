package com.now.nowbot.config;

import com.now.nowbot.listener.MessageListener;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.BotFactory;
import net.mamoe.mirai.utils.BotConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "nowbot.config")
public class NowbotConfig {
    public static final Logger log = LoggerFactory.getLogger(NowbotConfig.class);

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

    public static List<Long> SUPER_USER;
    @Value("#{'${mirai.superuser}'.split(',')}")
    public void setSuperUser(long[] users){
        SUPER_USER = new ArrayList<>();
        for (long us : users){
            SUPER_USER.add(us);
        }
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
    @Value("${mirai.start}")
    boolean isStart;
    @Bean
    public Bot bot(){
        BotConfiguration botConfiguration = new BotConfiguration();
        botConfiguration.setCacheDir(new File(BOT_PATH));
        botConfiguration.setHeartbeatStrategy(BotConfiguration.HeartbeatStrategy.STAT_HB);
        botConfiguration.setProtocol(BotConfiguration.MiraiProtocol.ANDROID_PAD);
        botConfiguration.setWorkingDir(new File(BOT_PATH));
        File logdir = new File(BOT_PATH+"log");
        if (!logdir.isDirectory()) logdir.mkdirs();
        botConfiguration.redirectBotLogToDirectory(logdir);
        botConfiguration.redirectNetworkLogToDirectory(logdir);
        botConfiguration.fileBasedDeviceInfo();
        botConfiguration.enableContactCache();
        botConfiguration.getContactListCache().setSaveIntervalMillis(60000*30);
        Bot bot = BotFactory.INSTANCE.newBot(NowbotConfig.QQ,NowbotConfig.PASSWORD,botConfiguration);
        if (isStart) bot.login();
        bot.getEventChannel().registerListenerHost(new MessageListener());

        return bot;
    }
}
