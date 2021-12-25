package com.now.nowbot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Primary;

@Primary
@ConfigurationProperties(prefix = "botfile")
public class FileConfig {
    String root;
    String bind;
    String mirai;
    String bgdir;
    String font;
    String imgbuffer;
    String osuid;

    public String getRoot() {
        return root;
    }

    public void setRoot(String root) {
        this.root = root;
    }

    public String getBind() {
        return bind;
    }

    public void setBind(String bind) {
        this.bind = bind;
    }

    public String getMirai() {
        return mirai;
    }

    public void setMirai(String mirai) {
        this.mirai = mirai;
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

    public String getOsuid() {
        return osuid;
    }

    public void setOsuid(String osuid) {
        this.osuid = osuid;
    }
}
