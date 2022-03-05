package com.now.nowbot.util;

import okhttp3.*;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class OsuMapDownloadUtil {
    public static final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();

    Cookie s = Cookie.parse(HttpUrl.parse("osu.ppy.sh"),"XSRF-TOKEN");
//    IKXzWuvwgYM4BhFn3QYFIKQT0otyQXUVRpkbbCiE

    void prs() throws IOException {
        var cail = httpClient;
        RequestBody body = new FormBody.Builder()
                .add("wd","hello")
                .add("ie","utf-8")
                .build();
        Request request = new Request.Builder()
                .url("http://www.baidu.com/s")
                .post(RequestBody.create(MediaType.parse("text/plain"),"wd=hello"))
                .build();

        Response response = cail.newCall(request).execute();
        var date = response.body().string();

        System.out.println(date);
    }
    public static void main(String[] args) {
        var cail = httpClient;
        var c1 =
                new Cookie.Builder().path("/").domain(".ppy.sh").name("XSRF-TOKEN").value("")
                        .httpOnly().secure().build();
        var c2 =
                new Cookie.Builder().path("/").domain(".ppy.sh").name("osu_session").value("")
                        .httpOnly().secure().build();
        cail.cookieJar().saveFromResponse(HttpUrl.parse("ppy.sh"), List.of(c1,c2));
        Request request = new Request.Builder()
                .url("https://osu.ppy.sh/beatmapsets/17331/download")
                .get()
                .build();
        Response response = null;
        try {
            response = cail.newCall(request).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(cail.cookieJar().loadForRequest(request.url()).size());
        try {
            System.out.println(cail.cookieJar().loadForRequest(request.url()).get(0).value());
            System.out.println(cail.cookieJar().loadForRequest(request.url()).get(1).value());
        } catch (Exception e) {
            System.out.println("no");
        }
        var ss= response.headers().values("set-cookie");

        ss.forEach(System.out::println);
        try {
            System.out.println(response.body().string());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}