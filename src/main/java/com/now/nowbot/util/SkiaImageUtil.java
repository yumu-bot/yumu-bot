package com.now.nowbot.util;

import com.now.nowbot.config.NowbotConfig;
import io.github.humbleui.skija.*;
import io.github.humbleui.skija.svg.SVGDOM;
import io.github.humbleui.types.RRect;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SkiaImageUtil {
    static MessageDigest MD;

    static {
        try {
            MD = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String path = "https://git.365246692.xyz/bot/nowbot_image";
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        md.update(path.getBytes());
        md.getAlgorithm();
        System.out.println(new BigInteger(1, md.digest()).toString(16));
    }

    /**
     * 加载网络图片,优先从本地加载图片
     *
     * @param path
     * @return
     * @throws IOException
     */
    public static Image getImageFronNetworkWithCache(String path) throws IOException {
        if (MD == null) {
            return null;
        }
        MD.update(path.getBytes());
        java.nio.file.Path pt = java.nio.file.Path.of(NowbotConfig.IMGBUFFER_PATH + new BigInteger(1, MD.digest()).toString(16));

        if (Files.isRegularFile(pt)) {
            MD.reset();
            return Image.makeFromEncoded(Files.readAllBytes(pt));
        } else {
            URL url = new URL(path);
            HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
            httpConn.connect();
            InputStream cin = httpConn.getInputStream();
            byte[] date = cin.readAllBytes();
            cin.close();
            Files.createFile(pt);
            Files.write(pt, date);
            return Image.makeFromEncoded(date);
        }
    }

    public static String getImageCachePath(String path) throws IOException {
        if (MD == null) {
            return null;
        }
        MD.update(path.getBytes());
        java.nio.file.Path pt = java.nio.file.Path.of(NowbotConfig.IMGBUFFER_PATH + new BigInteger(1, MD.digest()).toString(16));

        if (Files.isRegularFile(pt)) {
            MD.reset();
        } else {
            URL url = new URL(path);
            HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
            httpConn.connect();
            InputStream cin = httpConn.getInputStream();
            byte[] date = cin.readAllBytes();
            cin.close();
            Files.createFile(pt);
            Files.write(pt, date);
        }
        return pt.toAbsolutePath().toString();
    }

    /**
     * 加载网络图片,无缓存
     *
     * @param path
     * @return
     * @throws IOException
     */
    public static Image getImageFronNetwork(String path) throws IOException {
        URL url = new URL(path);
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        httpConn.connect();
        InputStream cin = httpConn.getInputStream();
        byte[] date;
        try {
            date = cin.readAllBytes();
        } finally {
            cin.close();
        }
        return Image.makeFromEncoded(date);
    }

    /**
     * 加载本地图片
     *
     * @param paths
     * @return
     * @throws IOException
     */
    public static Image getImage(Path... paths) throws IOException {
        if (paths.length == 0) throw new RuntimeException("error: no path");
        Path path = paths[0];
        for (int i = 1; i < paths.length; i++) {
            path = path.resolve(paths[i]);
        }
        var data = Files.readAllBytes(path);
        return Image.makeFromEncoded(data);
    }

    /**
     * 加载图片,通过判断"http"开头 加载网络或本地图片
     *
     * @param path url/路径
     * @return 图片
     */
    public static Image getImage(String path) throws IOException {
        if (path.trim().startsWith("http")) {
            return getImageFronNetworkWithCache(path);
        } else {
            var data = Files.readAllBytes(Path.of(path));
            return Image.makeFromEncoded(data);
        }
    }

    public static Image getImage(Path path) throws IOException {
        var data = Files.readAllBytes(path);
        return Image.makeFromEncoded(data);
    }

    /***
     * 缩放图形 根据目标大小缩放(直接绘制建议使用 drawScaleImage)
     * @param image 原图
     * @param width 目标宽度
     * @param height 目标高度
     * @return 目标图
     */
    public static Image getScaleImage(Image image, int width, int height) {
        if (image.getWidth() == width || image.getHeight() == height) return image;
        Image img = null;
        try (Surface sms = Surface.makeRasterN32Premul(width, height)) {
            sms.getCanvas()
                    .setMatrix(Matrix33.makeScale(1f * width / image.getWidth(), 1f * height / image.getHeight()))
                    .drawImage(image, 0, 0);
            img = sms.makeImageSnapshot();
        }
        return img;
    }

    /**
     * 缩放图形 跟据比例缩放
     *
     * @param image 原图
     * @param scale 缩放比例
     * @return 目标图
     */
    public static Image getScaleImage(Image image, float scale) {
        Image img = null;
        try (Surface sms = Surface.makeRasterN32Premul(Math.round(image.getWidth() * scale), Math.round(image.getHeight() * scale))) {
            sms.getCanvas().setMatrix(Matrix33.makeScale(scale)).drawImage(image, 0, 0);
            img = sms.makeImageSnapshot();
        }
        return img;
    }

    /***
     * 按比例缩放并裁切中间位置 得到的恰好是最贴近目标尺寸的图
     * @param img 原图
     * @param w 目标宽度
     * @param h 目标高度
     * @return 缩放并裁切后的图形
     */
    public static Image getScaleCenterImage(Image img, int w, int h) {
        if (img.getWidth() == w || img.getHeight() == h) return img;
        try (Surface surface = Surface.makeRasterN32Premul(w, h)) {
            var canvas = surface.getCanvas();
            if (1f * img.getWidth() / img.getHeight() < 1f * w / h) {
                //当原图比目标高
                canvas.setMatrix(Matrix33.makeScale(1f * w / img.getWidth(), 1f * w / img.getWidth()));
                //与下面同理
                canvas.translate(0, -0.5f * (1f * img.getHeight() * w / img.getWidth() - h) / w * img.getWidth());
                canvas.drawImage(img, 0, 0);
            } else {
                //当原图比目标宽
                canvas.setMatrix(Matrix33.makeScale(1f * h / img.getHeight(), 1f * h / img.getHeight()));
                //居中偏移 缩放比例(h/img.getHeight()) 后的宽度(img.getWidth()*h/img.getHeight()) 与 裁剪宽度差的一半
                canvas.translate(-0.5f * (1f * img.getWidth() * h / img.getHeight() - w) / h * img.getHeight(), 0);
                canvas.drawImage(img, 0, 0);
            }
            return surface.makeImageSnapshot();
        }
    }

    /***
     * 剪切矩形 暴力裁切,不足的部分为白色,裁切范围尽量不要超过图片尺寸(如果直接绘制建议使用 drawCutImage)
     * @param image 原图
     * @param left 左顶点
     * @param top 上顶点
     * @param width 裁切取得宽度
     * @param height 裁切取得高度
     * @return 裁切后的图
     */
    public static Image getCutImage(Image image, int left, int top, int width, int height) {
        Image img;
        try (Surface sms = Surface.makeRasterN32Premul(width, height)) {
            sms.getCanvas().clear(Color.makeRGB(255, 255, 255));
            sms.getCanvas().drawImage(image, -1 * left, -1 * top);
            img = sms.makeImageSnapshot();
        }
        return img;
    }

    /***
     * 得到圆角矩形 如果直接绘制建议使用 drawRRectImage
     * @param image 原图
     * @param r 圆角半径
     * @return 圆角矩形图
     */
    public static Image getRRectImage(Image image, float r) {
        return getRRectImage(image, image.getWidth(), image.getHeight(), r);
    }

    /**
     * 剪切到指定大小的圆角矩形
     *
     * @param image 原图
     * @param w
     * @param h
     * @param r
     * @return
     */
    public static Image getRRectImage(Image image, float w, float h, float r) {
        Image img;
        try (Surface surface = Surface.makeRasterN32Premul(((int) w), ((int) h))) {
            var canvas = surface.getCanvas();
            canvas.clipRRect(RRect.makeNinePatchXYWH(0, 0, w, h, r, r, r, r), true);
            canvas.drawImage(image, 0, 0);
            img = surface.makeImageSnapshot();
        }
        return img;
    }

    /***
     * 下载svg 大概不用再这里
     * @param path 下载url
     * @return svg对象
     * @throws IOException
     */
    public static SVGDOM getSVG(String path) throws IOException {
        URL url = new URL(path);
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        httpConn.connect();
        InputStream cin = httpConn.getInputStream();
        byte[] svgbytes;
        try {
            svgbytes = cin.readAllBytes();
        } finally {
            cin.close();
        }
        return new SVGDOM(Data.makeFromBytes(svgbytes));
    }

}
