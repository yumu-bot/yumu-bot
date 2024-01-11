package com.now.nowbot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Primary;

@Primary
@ConfigurationProperties(prefix = "botfile")
public class FileConfig {
    /**
     * 运行的位置
     */
    String root = "/tmp/bot";
    /**
     * 素材资源文件
     */
    String bgdir = "/tmp/bot/bg";
    /**
     * 字体文件资源
     */
    String font = "/tmp/bot/font";
    /**
     * 资源缓存路径
     */
    String imgbuffer = "/tmp/bot/imgbuffer";
    /**
     * 图片资源文件
     */
    String exportFile = "/tmp/bot/bg/ExportFileV3";
    /**
     * 缓存 .osu 文件的路径
     */
    String osuFilePath = "/tmp/bot/osufile";

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

    public String getExportFile() {
        return exportFile;
    }

    public void setExportFile(String exportFile) {
        this.exportFile = exportFile;
    }
}
