package com.now.nowbot.qq.message;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

public class Message {
    public String toString() {
        return "";
    }

    public String getCQ() {
        return "";
    }

    public JsonMessage toJson() {
        return null;
    }

    @JsonInclude(value = JsonInclude.Include.NON_EMPTY, content = JsonInclude.Include.NON_EMPTY)
    public class JsonMessage {
        public String              type;
        public Map<String, Object> data;

        public JsonMessage(String type, Map<String, Object> data) {
            this.type = type;
            this.data = data;
        }

        public JsonMessage() {
            this.type = null;
            this.data = null;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Map<String, Object> getData() {
            return data;
        }

        public void setData(Map<String, Object> data) {
            this.data = data;
        }
    }
}
