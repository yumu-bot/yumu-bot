package com.now.nowbot.config;

import com.now.nowbot.util.JacksonUtil;
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
import org.springframework.stereotype.Component;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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

    /**
     * 不再支持 socket5, 临时不删
     */
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
    public JsonMapper objectMapper() {
        return JacksonUtil.INSTANCE.getMapper();
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
}
