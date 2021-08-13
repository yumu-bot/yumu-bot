package com.now.nowbot.config;


import net.mamoe.mirai.Bot;
import net.mamoe.mirai.BotFactory;
import net.mamoe.mirai.event.Event;
import net.mamoe.mirai.event.GlobalEventChannel;
import net.mamoe.mirai.utils.BotConfiguration;
import net.mamoe.mirai.utils.DeviceInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Configuration
public class BotConfig {

    @Value("${mirai.start}")
    boolean isStart;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    /***
     * 注册bot&bot事件监听添加
     * @return
     */
    @Bean
    public Bot bot1(){
        miraiBotConfig config = new miraiBotConfig(NowbotConfig.BOT_PATH);
        Bot bot = BotFactory.INSTANCE.newBot(NowbotConfig.QQ,NowbotConfig.PASSWORD,config);
        if(isStart)
            bot.login();
        //事件监听
        GlobalEventChannel.INSTANCE.subscribeAlways(Event.class, event -> {
            applicationEventPublisher.publishEvent(event);
        });
        return bot;
    }

}
class miraiBotConfig extends BotConfiguration {

        miraiBotConfig(String dir){
            super();
            setHeartbeatStrategy(HeartbeatStrategy.STAT_HB);
            setProtocol(MiraiProtocol.ANDROID_PAD);
            setDeviceInfo((bot) -> {
                var dvi = DeviceInfo.from(new File(dir+"device.json"));
                return dvi;
            });
            setCacheDir(new File(dir));
//        setLoginSolver(new YourLoginSolver());
//        noNetworkLog()
//        noBotLog();
            File logdir = new File(dir+"log");
            if (!logdir.isDirectory()) logdir.mkdirs();
            redirectBotLogToDirectory(logdir);
            redirectNetworkLogToDirectory(logdir);
            fileBasedDeviceInfo();
            enableContactCache();
            getContactListCache().setSaveIntervalMillis(60000*30);
        }
}

