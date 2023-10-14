package com.now.nowbot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Primary;

@Primary
@ConfigurationProperties(prefix = "botfile")
public class FileConfig {
    String root;
    String bgdir;
    String font;
    String imgbuffer;

    /**
     * 缓存 .osu 文件的路径
     */
    String osuFilePath = "/osufile";

    public String getRoot() {
        return root;
    }

    public void setRoot(String root) {
        this.root = root;
    }

    public String getBgdir() {
        return bgdir;
    }

    public void setBgdir(String bgdir) {
        this.bgdir = bgdir;
    }

    public String getFont() {
        return font;
    }

    public void setFont(String font) {
        this.font = font;
    }

    public String getImgbuffer() {
        return imgbuffer;
    }

    public void setImgbuffer(String imgbuffer) {
        this.imgbuffer = imgbuffer;
    }


    public String getOsuFilePath() {
        return osuFilePath;
    }

    public void setOsuFilePath(String osuFilePath) {
        this.osuFilePath = osuFilePath;
    }
}
