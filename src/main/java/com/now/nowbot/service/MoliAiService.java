package com.now.nowbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MessageEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

public class MoliAiService {
    RestTemplate restTemplate;
    String url = "https://i.mly.app/reply";
    HttpHeaders headers = new HttpHeaders();
    public static record Friend(String sg, String name, Long id){};
    public static record Group(String sg, String name, Long id, String groupName, Long groupId){};

    public static Friend getFriend(MessageEvent e){
        return new Friend(e.getMessage().contentToString(), e.getSenderName(), e.getSender().getId());
    }
    public static Group getGroup(GroupMessageEvent e){
        return new Group(e.getMessage().contentToString(), e.getSenderName(), e.getSender().getId(), e.getGroup().getName(), e.getGroup().getId());
    }
    @Autowired
    public MoliAiService(RestTemplate restTemplate){
        this.restTemplate = restTemplate;
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Api-Key", "438983az3zr8riuk");
        headers.set("Api-Secret", "vissf8jj");
    }

    public String[] getMsg(Friend friend){
        HttpEntity<Friend> httpEntity = new HttpEntity<>(friend, headers);
        ResponseEntity<JsonNode> c = restTemplate.exchange(url, HttpMethod.POST, httpEntity, JsonNode.class);
        var data = c.getBody();
        if ("00000".equals(data.get("code").asText())) {
            var req = data.get("data");
            String[] strs = new String[req.size()];
            for (int i = 0; i < req.size(); i++) {
                strs[i] = req.get(i).get("content").asText();
            }
            return strs;
        }else {
            throw new RuntimeException(data.get("message").asText());
        }
    }
    public String[] getMsg(Group group){
//        post body = new post();
        HttpEntity<Group> httpEntity = new HttpEntity<>(group, headers);
        ResponseEntity<JsonNode> c = restTemplate.exchange(url, HttpMethod.POST, httpEntity, JsonNode.class);
        var data = c.getBody();
        if ("00000".equals(data.get("code").asText())) {
            var req = data.get("data");
            String[] strs = new String[req.size()];
            for (int i = 0; i < req.size(); i++) {
                strs[i] = req.get(i).get("content").asText();
            }
            return strs;
        }else {
            throw new RuntimeException(data.get("message").asText());
        }
    }
    public class post{
        String content;
        int type;
        Long from;
        String fromName;
        Long to;
        String toName;

        public post(int type, Long from, String fromName) {
            this.content = "";
            this.type = type;
            this.from = from;
            this.fromName = fromName;
            this.to = 0L;
            this.toName="";
        }

        public post(int type, Long from, String fromName, Long to, String toName) {
            this.content = "";
            this.type = type;
            this.from = from;
            this.fromName = fromName;
            this.to = to;
            this.toName = toName;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getContent() {
            return content;
        }

        public int getType() {
            return type;
        }

        public Long getFrom() {
            return from;
        }

        public String getFromName() {
            return fromName;
        }

        public Long getTo() {
            return to;
        }

        public String getToName() {
            return toName;
        }
    }
//https://mly.app/profile/index.html#/dashboard

}
