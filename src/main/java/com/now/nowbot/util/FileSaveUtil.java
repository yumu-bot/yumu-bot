package com.now.nowbot.util;

import io.ktor.utils.io.core.Input;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.UploadObjectArgs;
import io.minio.errors.*;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class FileSaveUtil {
    private static MinioClient client = MinioClient.builder()
            .endpoint("http://192.168.243.253:8666")
            .credentials("spring_test", "test123456789")
            .build();

    private static String getUrl (String name) {
        return String.format("http://minio.365246692.xyz:52111/%s/%s", bucket, name);
    }
    private static String getUrlN (String name) {
        return String.format("http://192.168.243.253:8666/%s/%s", bucket, name);
    }
    private static final String bucket = "nowbot";

    private static UploadObjectArgs upload (String name, String filePath) throws IOException {
        return UploadObjectArgs.builder()
                .bucket(bucket)
                .object(name)
                .filename(filePath)
                .build();
    }
    private static PutObjectArgs uploded (String name, InputStream in) throws IOException {
        return PutObjectArgs.builder()
                .bucket(bucket)
                .object(name)
                .stream(in, in.available(), -1)
                .build();
    }
    private static PutObjectArgs uploded (String name, InputStream in, int length) {
        return PutObjectArgs.builder()
                .bucket(bucket)
                .object(name)
                .stream(in, length, -1)
                .build();
    }

    public static void main(String[] args) {
        try {
            client.uploadObject(upload("bot.jar", "/home/spring/cache/nowbot/nowbot-linux.jar"));
            System.out.println(getUrlN("bot.jar"));
        } catch (ErrorResponseException | NoSuchAlgorithmException | ServerException | InsufficientDataException |
                 InvalidKeyException | InvalidResponseException | InternalException | IOException | XmlParserException e) {
            throw new RuntimeException(e);
        }
    }
}
