package com.now.nowbot.qq.message;

public class VoiceMessage extends Message {
    final byte[] data;
    public VoiceMessage(byte[] data) {
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public String toString() {
        return "[语音]";
    }
}
