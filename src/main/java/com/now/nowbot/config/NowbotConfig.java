package com.now.nowbot.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.now.nowbot.throwable.RequestException;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
@Configuration
public class NowbotConfig {
    private static final Logger log = LoggerFactory.getLogger(NowbotConfig.class);
    public static        String RUN_PATH;
    public static        String BOT_PATH;
    public static        String FONT_PATH;
    public static        String BG_PATH;
    public static        String IMGBUFFER_PATH;
    public static        String OSU_ID;
    public static        int    PORT;

    public static long    QQ;
    public static String  PASSWORD;
    public static boolean QQ_LOGIN;


    @Autowired
    public NowbotConfig(FileConfig fileConfig, QQConfig qqConfig) {
        RUN_PATH = createDir(fileConfig.root);
        BOT_PATH = createDir(fileConfig.mirai);
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
    @Primary
    public ObjectMapper jacksonObjectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper mapper = builder.createXmlMapper(false).build();
//        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper
                .registerModule(new Jdk8Module())
                .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        return mapper;
    }

    @Bean
    public NoProxyRestTemplate noProxyRestTemplate() {
        var tempFactory = new OkHttp3ClientHttpRequestFactory();
        tempFactory.setConnectTimeout(120 * 1000);
        tempFactory.setReadTimeout(120 * 1000);
        var template = new NoProxyRestTemplate(tempFactory);
        template.setErrorHandler(new DefaultResponseErrorHandler() {
            public void handleError(ClientHttpResponse response, HttpStatus statusCode) throws RequestException {
                throw new RequestException(response, statusCode);
            }
        });
        return template;
    }

    @Bean(name = {"restTemplate", "template"})
    public RestTemplate restTemplate() {
        var client = httpClient();

        var tempFactory = new OkHttp3ClientHttpRequestFactory(client);
        tempFactory.setConnectTimeout( 60 * 1000);
        tempFactory.setReadTimeout( 60 * 1000);
        var template = new RestTemplate(tempFactory);
        template.setErrorHandler(new DefaultResponseErrorHandler() {
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
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) return true;
        } catch (IOException e) {
            log.error("代理不可用", e);
        }
        return false;
    }

    @Bean
    public WebClient webCilent(WebClient.Builder builder)  {
        return builder
                .baseUrl("https://osu.ppy.sh/")
                .build();
    }


    @Value("${server.port}")
    public void setPORT(Integer port) {
        PORT = port;
    }
}
