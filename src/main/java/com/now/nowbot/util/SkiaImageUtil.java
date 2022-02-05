package com.now.nowbot.util;

import com.now.nowbot.config.NowbotConfig;
import org.jetbrains.skija.Data;
import org.jetbrains.skija.Image;
import org.jetbrains.skija.RRect;
import org.jetbrains.skija.Surface;
import org.jetbrains.skija.svg.SVGDOM;

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

    /**
     * 加载网络图片,优先从本地加载图片
     *
     * @param path
     * @return
     * @throws IOException
     */
    public static Image getImageFronNetworkWithCache(String path) throws IOException {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        md.update(path.getBytes());
        md.getAlgorithm();
        java.nio.file.Path pt = java.nio.file.Path.of(NowbotConfig.IMGBUFFER_PATH + new BigInteger(1, md.digest()).toString(16));

        if (Files.isRegularFile(pt)) {
            md.reset();
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
            ;
            var data = Files.readAllBytes(Path.of(path));
            return Image.makeFromEncoded(data);
        }
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

    /***
     * 剪切圆角矩形 如果直接绘制建议使用 drawRRectImage
     * @param image
     * @param r
     * @return
     */
    public static Image getRRectImage(Image image, float r) {
        return getRRectImage(image,image.getWidth(),image.getHeight(),r);
    }
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
}
