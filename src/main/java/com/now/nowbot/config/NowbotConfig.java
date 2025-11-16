package com.now.nowbot.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.now.nowbot.service.DiscordService;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Component
@Configuration
public class NowbotConfig {
    private static final Logger log = LoggerFactory.getLogger(NowbotConfig.class);

    /**
     * bot 运行目录
     */
    public static String RUN_PATH;
    /**
     * 字体资源文件
     */
    public static String FONT_PATH;
    /**
     * 资源文件
     */
    public static String BG_PATH;
    /**
     * 素材资源文件
     */
    public static String EXPORT_FILE_PATH;
    /**
     * 网络图片 本地缓存
     */
    public static String IMGBUFFER_PATH;
    public static int    PORT;
    @Value("${spring.proxy.type:'HTTP'}")
    public        String proxyType;
    @Value("${spring.proxy.host:'localhost'}")
    public        String proxyHost;
    @Value("${spring.proxy.port:7890}")
    public        int    proxyPort;

    @Autowired
    public NowbotConfig(FileConfig fileConfig) {
        RUN_PATH = createDir(fileConfig.root);
        FONT_PATH = createDir(fileConfig.font);
        BG_PATH = createDir(fileConfig.bgdir);
        EXPORT_FILE_PATH = createDir(fileConfig.exportFile);
        IMGBUFFER_PATH = createDir(fileConfig.imgbuffer);
    }

    @Bean
    @Primary
    public ObjectMapper jacksonObjectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper mapper = builder.createXmlMapper(false).build();
//        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.registerModule(new Jdk8Module()).setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
              .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        return mapper;
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
                log.error("{}创建失败", path, e);
            }
        }
        return path;
    }

    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }

    static boolean testProxy(OkHttpClient client) {
        Request request = new Request.Builder().url("https://osu.ppy.sh/users/17064371/scores/best?mode=osu&limit=1&offset=0").build();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) return true;
        } catch (IOException e) {
            log.error("代理不可用", e);
        }
        return false;
    }

    @Bean
    public JDA getDiscord(List<ListenerAdapter> listenerAdapters, NowbotConfig config, DiscordConfig discordConfig, ThreadPoolTaskExecutor botAsyncExecutor) {
        var discordService = new DiscordService(discordConfig, config, listenerAdapters, botAsyncExecutor);

        return discordService.getJDA();
    }

    /*

    //@Bean
    //@DependsOn("discordConfig")
    public JDA jda(List<ListenerAdapter> listenerAdapters, OkHttpClient okHttpClient, NowbotConfig config, DiscordConfig discordConfig, ThreadPoolTaskExecutor botAsyncExecutor) {
        if (discordConfig.getToken().equals("*")) return null;
        WebSocketFactory factory = new WebSocketFactory();
        var proxy = factory.getProxySettings();
        if (config.proxyPort != 0) proxy.setHost("localhost").setPort(config.proxyPort);
        JDA jda;
        try {
            var scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(0, THREAD_FACTORY);
            jda = JDABuilder.createDefault(discordConfig.getToken()).setHttpClient(okHttpClient)
                            .setWebsocketFactory(factory).addEventListeners(listenerAdapters.toArray())
                            .setCallbackPool(botAsyncExecutor.getThreadPoolExecutor())
                            .setGatewayPool(scheduledThreadPoolExecutor).setRateLimitPool(scheduledThreadPoolExecutor)
                            .setEventPool(botAsyncExecutor.getThreadPoolExecutor())
                            .setAudioPool(scheduledThreadPoolExecutor).build();
            jda.awaitReady();
        } catch (Exception e) {
            log.error("create jda error: {}", e.getMessage());
            return null;
        }

        for (Command command : jda.retrieveCommands().complete()) {
            command.delete().complete();
        }
        for (Method declaredMethod : BotWebApi.class.getDeclaredMethods()) {
            DiscordParam methodAnnotation = declaredMethod.getAnnotation(DiscordParam.class);
            if (methodAnnotation == null) {
                continue;
            }
            String name = methodAnnotation.name();
            SlashCommandData commandData =
                    Commands.slash((discordConfig.getCommandSuffix() + name).toLowerCase(), methodAnnotation.desp());
            for (Parameter parameter : declaredMethod.getParameters()) {
                DiscordParam parameterAnnotation = parameter.getAnnotation(DiscordParam.class);
                if (parameterAnnotation == null) {
                    continue;
                }
                final OptionData optionData = getOptionData(parameter, parameterAnnotation);
                commandData.addOptions(optionData);
            }
            jda.upsertCommand(commandData).complete();
        }
        log.info("jda init ok");

        return jda;
    }

    @NotNull
    private static OptionData getOptionData(Parameter parameter, DiscordParam parameterAnnotation) {
        OptionType optionType;
        Class<?> type = parameter.getType();
        if (type.equals(int.class) || type.equals(Integer.class)) {
            optionType = OptionType.INTEGER;
        } else if (type.equals(boolean.class) || type.equals(Boolean.class)) {
            optionType = OptionType.BOOLEAN;
        } else {
            optionType = OptionType.STRING;
        }
        String parameterName = parameterAnnotation.name();
        OptionData optionData = new OptionData(optionType, parameterName.toLowerCase(), parameterAnnotation.desp());
        if (parameterName.equals("mode")) {
            optionData.addChoice("OSU", "OSU");
            optionData.addChoice("TAIKO", "TAIKO");
            optionData.addChoice("CATCH", "CATCH");
            optionData.addChoice("MANIA", "MANIA");
        }
        optionData.setRequired(parameterAnnotation.required());
        return optionData;
    }

    @Value("${server.port}")
    public void setPORT(Integer port) {
        PORT = port;
    }

    @Bean
    public CacheManager cacheManager(Executor mainExecutor) {
        var caffeine =
                Caffeine.newBuilder().executor(mainExecutor).expireAfterAccess(5, TimeUnit.SECONDS).maximumSize(60);
        var manager = new CaffeineCacheManager();
        manager.setCaffeine(caffeine);
        manager.setAllowNullValues(true);
        return manager;
    }

     */
}
