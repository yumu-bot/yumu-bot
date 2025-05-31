package com.now.nowbot.service.biliApiService;

import com.fasterxml.jackson.databind.JsonNode;
import com.now.nowbot.model.live.LiveRoom;
import com.now.nowbot.model.live.LiveStatus;
import com.now.nowbot.qq.Bot;
import com.now.nowbot.util.QQMsgUtil;
import com.now.nowbot.util.SkiaImageUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.*;

@Service
public class BiliApiLegacyService {
    Logger log = LoggerFactory.getLogger(BiliApiLegacyService.class);
    private static final String ROOM_API         = "http://api.live.bilibili.com/room/v1/Room/room_init?id=";
    private static final String USER_API         = "http://api.live.bilibili.com/live_user/v1/Master/info?uid=";
    private static final String USER_ALLINFO_API = "https://api.bilibili.com/x/space/acc/info?mid=";
    private static final String ALL_ROOM_API     = "http://api.live.bilibili.com/room/v1/Room/get_status_info_by_uids";
    @Resource
    WebClient webClient;
    Bot bot;

    private static final HashMap<Long, Long> sendGroupMap = new HashMap<>();
    private static final Set<Long> lastList = new HashSet<>();

    public List<LiveRoom> getLiveRooms(Long[] roomid) {
        //live_status 0：未开播 1：直播中 2：轮播中   live_time 秒时间戳
        var data = webClient.post().uri(ALL_ROOM_API)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("uids", roomid))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block()
                .get("data");
        List<LiveRoom> rooms = new ArrayList<>();
        for (JsonNode room : data) {
            var r = new LiveRoom(room);
            rooms.add(r);
        }
        return rooms;
    }

    public void sendmsg(LiveRoom room, Long groupId, boolean isOpen) {
        var group = bot.getGroup(groupId);
        if (group != null) {
            if (isOpen) {
                StringBuffer sb = new StringBuffer();
                sb.append(room.getUserName()).append("爷爷开始直播了!").append('\n');
                sb.append(room.getRoomName()).append("-").append(room.getAreaName()).append('\n');
                sb.append("快戳我围观->https://live.bilibili.com/").append(room.getRoomId()).append('\n');
                byte[] img = null;
                try {
                    img = SkiaImageUtil.getImage(room.getKeyframeUrl()).encodeToData().getBytes();
                } catch (IOException e) {
                    log.error("直播画面拉取异常", e);
                }
                if (img != null) {
                    QQMsgUtil.sendImageAndText(group, img, sb.toString());
                } else {
                    group.sendMessage(sb.toString());
                }
            } else {
                StringBuffer sb = new StringBuffer();
                sb.append(room.getUserName()).append("爷爷的直播结束了>_<").append('\n');
                sb.append(room.getRoomName()).append("-").append(room.getAreaName()).append("  已结束").append('\n');
                sb.append("快戳我关注直播间!->https://live.bilibili.com/").append(room.getRoomId()).append('\n');
                byte[] img = null;
                try {
                    img = SkiaImageUtil.getImage(room.getKeyframeUrl()).encodeToData().getBytes();
                } catch (IOException e) {
                    log.error("直播画面拉取异常", e);
                }
                if (img != null) {
                    QQMsgUtil.sendImageAndText(group, img, sb.toString());
                } else {
                    group.sendMessage(sb.toString());
                }
            }
        }
    }

    public void check() {
        Long[] roomsId = sendGroupMap.keySet().toArray(Long[]::new);
        var data = getLiveRooms(roomsId);
        for (var room : data) {
            if (lastList.contains(room.getUid()) && room.getStatus() != LiveStatus.OPEN) {
                lastList.remove(room.getUid());
                sendmsg(room, sendGroupMap.get(room.getUid()), false);
            } else if (!lastList.contains(room.getUid()) && room.getStatus() == LiveStatus.OPEN) {
                lastList.add(room.getUid());
                sendmsg(room, sendGroupMap.get(room.getUid()), true);
            }
        }
    }
}