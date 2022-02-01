package com.now.nowbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.now.nowbot.model.live.LiveRoom;
import com.now.nowbot.model.live.LiveStatus;
import com.now.nowbot.util.QQMsgUtil;
import com.now.nowbot.util.SkiaUtil;
import net.mamoe.mirai.Bot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BiliApiService {
    Logger log = LoggerFactory.getLogger(BiliApiService.class);
    private static final String ROOM_API = "http://api.live.bilibili.com/room/v1/Room/room_init?id=";
    private static final String USER_API = "http://api.live.bilibili.com/live_user/v1/Master/info?uid=";
    private static final String USER_ALLINFO_API = "https://api.bilibili.com/x/space/acc/info?mid=";
    private static final String ALL_ROOM_API = "http://api.live.bilibili.com/room/v1/Room/get_status_info_by_uids";
    RestTemplate restTemplate;
    Bot bot;

    private static final HashMap<String, Long> sendGroupMap = new HashMap<>();
    private static final List<String> lastList = new ArrayList<>();
    @Autowired
    public BiliApiService(RestTemplate restTemplate, Bot bot){
        this.restTemplate = restTemplate;
        this.bot = bot;
        sendGroupMap.put("545149341", 733244168L);
        sendGroupMap.put("73769122", 733244168L);
        sendGroupMap.put("14172231", 135214594L);
    }

    public List<LiveRoom> getLiveRooms(Long[] roomid){
        //live_status 0：未开播 1：直播中 2：轮播中   live_time 秒时间戳
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity httpEntity = new HttpEntity(Map.of("uids",roomid),headers);
        var data = restTemplate.postForObject(ALL_ROOM_API, httpEntity, JsonNode.class).get("data");
        data.get("data");
        List<LiveRoom> rooms = new ArrayList<>();
        for (JsonNode room : data){
            var r = new LiveRoom(room);
            rooms.add(r);
            System.out.println(r);
        }
        return rooms;
    }

    public void sendmsg(LiveRoom room, Long groupId, boolean isOpen){
        var group = bot.getGroup(groupId);
        if (group != null) {
            if (isOpen){
                StringBuffer sb = new StringBuffer();
                sb.append(room.getUserName()).append("爷爷开始直播了!").append('\n');
                sb.append(room.getRoomName()).append("-").append(room.getAreaName()).append('\n');
                sb.append("快戳我围观->https://live.bilibili.com/").append(room.getRoomId()).append('\n');
                QQMsgUtil.sendTextAndImage(group, sb.toString(), SkiaUtil.lodeNetWorkImage(room.getKeyframeUrl()).encodeToData().getBytes());
            }
        }
    }

    public void check(){
        var set = sendGroupMap.keySet();
        Long[] roomsId = new Long[set.size()];
        var d = set.iterator();
        for (int i = 0; d.hasNext(); i++) {
            roomsId[i] = Long.parseLong(d.next());
        }
        var data = getLiveRooms(roomsId);
        data.forEach(room -> {
            if (lastList.contains(room.getUid()) && room.getStatus() != LiveStatus.CLOSE){
                lastList.remove(room.getUid());
                sendmsg(room, sendGroupMap.get(room.getUid()),false);
//                log.info("close->{}, has->{}",room.getUid(),lastList.contains(room.getUid()));
            }
            if (!lastList.contains(room.getUid()) && room.getStatus() == LiveStatus.OPEN){
                lastList.add(room.getUid());
                sendmsg(room, sendGroupMap.get(room.getUid()),true);
//                log.info("start->{}, has->{}",room.getUid(),lastList.contains(room.getUid()));
            }
//            log.info("直播开启的数量{}",lastList.size());
        });
    }
}