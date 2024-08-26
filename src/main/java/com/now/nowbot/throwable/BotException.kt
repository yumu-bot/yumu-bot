package com.now.nowbot.throwable;

public interface BotException {
    String getMessage();

    boolean hasImage();

    byte[] getImage();
}
