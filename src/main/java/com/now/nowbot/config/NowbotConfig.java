package com.now.nowbot.config;

import com.now.nowbot.listener.MessageListener;
import com.now.nowbot.listener.MiraiListener;
import com.now.nowbot.throwable.RequestException;
import kotlinx.coroutines.CoroutineScope;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.BotFactory;
import net.mamoe.mirai.auth.BotAuthorization;
import net.mamoe.mirai.event.ListenerHost;
import net.mamoe.mirai.utils.BotConfiguration;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;
import xyz.cssxsh.mirai.tool.FixProtocolVersion;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@Configuration
public class NowbotConfig {
    private static final Logger log = LoggerFactory.getLogger(NowbotConfig.class);
    public static        String RUN_PATH;
    public static        String BOT_PATH;
    public static        String BIN_PATH;
    public static        String FONT_PATH;
    public static        String BG_PATH;
    public static        String IMGBUFFER_PATH;
    public static        String OSU_ID;

    public static long    QQ;
    public static String  PASSWORD;
    public static boolean QQ_LOGIN;

    @Autowired
    public NowbotConfig(FileConfig fileConfig, QQConfig qqConfig) {
        RUN_PATH = createDir(fileConfig.root);
        BOT_PATH = createDir(fileConfig.mirai);
        BIN_PATH = createDir(fileConfig.bind);
        FONT_PATH = createDir(fileConfig.font);
        BG_PATH = createDir(fileConfig.bgdir);
        IMGBUFFER_PATH = createDir(fileConfig.imgbuffer);
        OSU_ID = createDir(fileConfig.osuid);

        QQ = qqConfig.qq;
        PASSWORD = qqConfig.password;
        QQ_LOGIN = qqConfig.login;
    }

    @Bean
    public OkHttpClient httpClient() {
        var clientBuilder = new OkHttpClient.Builder()
                .proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 7890)));
        return clientBuilder.build();
    }

    @Bean
    public RestTemplate restTemplate() {
        var client = httpClient();

        var tempFactory = new OkHttp3ClientHttpRequestFactory(client);
        tempFactory.setConnectTimeout(3 * 60 * 1000);
        tempFactory.setReadTimeout(3 * 60 * 1000);
        var template = new RestTemplate(tempFactory);
        template.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public void handleError(ClientHttpResponse response, HttpStatus statusCode) throws RequestException {
                throw new RequestException(response, statusCode);
            }
        });

//        List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
//        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
//        converter.setSupportedMediaTypes(Collections.singletonList(MediaType.ALL));
//        messageConverters.add(converter);
//
//        template.setMessageConverters(messageConverters);
        return template;
    }

    @Autowired
    MiraiListener messageListener;

    @Bean
    public Bot bot() {
//        FixProtocolVersion.update();
//        FixProtocolVersion.sync(BotConfiguration.MiraiProtocol.ANDROID_WATCH);
//        log.info("update version: {}", FixProtocolVersion.info());
        //创建bot配置类
        BotConfiguration botConfiguration = new BotConfiguration();
        //设置配置
        botConfiguration.setCacheDir(new File(BOT_PATH));
        botConfiguration.setHeartbeatStrategy(BotConfiguration.HeartbeatStrategy.STAT_HB);
        botConfiguration.setProtocol(BotConfiguration.MiraiProtocol.ANDROID_WATCH);
        botConfiguration.setWorkingDir(new File(BOT_PATH));

        File logdir = new File(BOT_PATH + "log");
        if (!logdir.isDirectory()) logdir.mkdirs();
        botConfiguration.redirectBotLogToDirectory(logdir);
        botConfiguration.redirectNetworkLogToDirectory(logdir);
        botConfiguration.fileBasedDeviceInfo();
        botConfiguration.enableContactCache();
        botConfiguration.getContactListCache().setSaveIntervalMillis(60000 * 30);
        //配置完成，注册bot                    BotAuthorization.Companion.byPassword()
        var auth = PASSWORD.equals("") ? BotAuthorization.Companion.byQRCode() : BotAuthorization.byPassword(PASSWORD);
        Bot bot = BotFactory.INSTANCE.newBot(NowbotConfig.QQ, auth, botConfiguration);
        //注册监听 messageListener需要继承SimpleListenerHost类
        bot.getEventChannel().parentScope((CoroutineScope) messageListener).registerListenerHost((ListenerHost) messageListener);
        return bot;
    }

    public static ApplicationContext applicationContext;

    @Autowired
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        NowbotConfig.applicationContext = applicationContext;
    }

    public String createDir(String path) {
        Path pt = Path.of(path);
        if (!Files.isDirectory(pt)) {
            try {
                Files.createDirectories(pt);
            } catch (IOException e) {
                log.error(BOT_PATH + "创建失败", e);
            }
        }
        return path;
    }

    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }

    static boolean testProxy(OkHttpClient client) {
        Request request = new Request.Builder()
                .url("https://osu.ppy.sh/users/17064371/scores/best?mode=osu&limit=1&offset=0")
                .build();
        try (Response response = client.newCall(request).execute()){
            if (response.isSuccessful()) return true;
        } catch (IOException e) {
            log.error("代理不可用", e);
        }
        return false;
    }
}