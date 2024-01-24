package com.now.nowbot.throwable;

public class TipsException extends Exception implements BotException {
    String message;
    byte[] image = null;

    public TipsException(String message) {
        setMessage(message);
    }

    public TipsException(byte[] image) {
        this.image = image;
    }
    public void setMessage(String message) {
        this.message = message;
    }

    public boolean hasImage() {
        return image == null;
    }

    public byte[] getImage() {
        return image;
    }

    @Override
    public String getMessage() {
        if (message != null) {
            return message;
        }
        return super.getMessage();
    }
}
