package com.now.nowbot.qq.message;

import java.net.URL;

public class ImageMessage extends Message {
    private enum Type {
        FILE, URL, BYTE_ARRAY
    }

    private final Type   type;
    final         String path;
    final         byte[] data;

    public ImageMessage(byte[] img) {
        data = img;
        path = null;
        type = Type.BYTE_ARRAY;
    }

    public ImageMessage(String path) {
        data = null;
        this.path = "file:///" + path;
        type = Type.FILE;
    }

    public ImageMessage(URL url) {
        data = null;
        path = url.toExternalForm();
        type = Type.URL;
    }

    public boolean isByteArray() {
        return type == Type.BYTE_ARRAY;
    }

    public byte[] getData() {
        return data;
    }

    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return "[图片]";
    }
}
