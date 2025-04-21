package com.now.nowbot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.nio.file.Path;

@Validated
@ConfigurationProperties(prefix = "yumu.maimai", ignoreInvalidFields = true)
public class DivingFishConfig {
    /**
     * 接口路径, 一般不用改
     */
    public static final String url   = "https://www.diving-fish.com/";

    public static final String token = "BMurCyOaA0cfist6VpNvb7ZXK5h1noSE";

    // D:/App2/[Projects]/yumu-bot-run/img/ExportFileV3/Maimai
    // /home/spring/work/img/ExportFileV3/Maimai
    public static final Path maimai = Path.of("/home/spring/work/img/ExportFileV3/Maimai");

    // D:/App2/[Projects]/yumu-bot-run/img/ExportFileV3/Chunithm
    // /home/spring/work/img/ExportFileV3/Chunithm
    public static final Path chunithm = Path.of("/home/spring/work/img/ExportFileV3/Chunithm");

    public String getUrl() {
        return url;
    }

    public String getToken() {
        return token;
    }

    public Path getMaimai() {
        return maimai;
    }

    public Path getChunithm() {
        return chunithm;
    }


}
