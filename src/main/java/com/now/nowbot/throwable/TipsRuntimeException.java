package com.now.nowbot.throwable;

import java.util.Objects;

public class TipsRuntimeException extends RuntimeException implements BotException {
    byte[] image = null;
    public TipsRuntimeException(String msg){
        super(msg);
    }

    public TipsRuntimeException(byte[] image) {
        this.image = image;
    }

    @Override
    public boolean hasImage() {
        return Objects.nonNull(image);
    }

    @Override
    public byte[] getImage() {
        return image;
    }
}
