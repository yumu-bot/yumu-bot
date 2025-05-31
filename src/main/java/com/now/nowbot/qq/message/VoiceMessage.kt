package com.now.nowbot.qq.message;

import com.now.nowbot.util.QQMsgUtil;

import java.util.Map;

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

    @Override
    public JsonMessage toJson() {
        return new JsonMessage("record", Map.of("file", STR."base64://\{QQMsgUtil.byte2str(getData())}"));
    }
}
