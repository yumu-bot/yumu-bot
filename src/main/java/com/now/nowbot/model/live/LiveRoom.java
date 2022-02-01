package com.now.nowbot.model.live;

import com.fasterxml.jackson.databind.JsonNode;

public class LiveRoom {
    String roomName;
    String userName;
    Long roomId;

    String fromUrl;
    //关键帧
    String keyframeUrl;
    LiveStatus status;
    String uid;
    //秒 时间戳
    Long liveTime;
    Integer online;

    String tags;
    String areaName;

    public LiveRoom() {
    }

    public LiveRoom(JsonNode room){
        setUserName(room.get("uname").asText("null"));
        setRoomName(room.get("title").asText("null"));
        setAreaName(room.get("area_name").asText("null"));
        setLiveTime(room.get("live_time").asLong());
        setOnline(room.get("online").asInt());
        setFromUrl(room.get("cover_from_user").asText());
        setKeyframeUrl(room.get("keyframe").asText());
        setStatus(LiveStatus.get(room.get("live_status").asInt()));
        setTags(room.get("tags").asText());
        setUid(room.get("uid").asLong());
        setRoomId(room.get("room_id").asLong());
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(Long uid) {
        this.uid = uid.toString();
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getFromUrl() {
        return fromUrl;
    }

    public void setFromUrl(String fromUrl) {
        this.fromUrl = fromUrl;
    }

    public String getKeyframeUrl() {
        return keyframeUrl;
    }

    public void setKeyframeUrl(String keyframeUrl) {
        this.keyframeUrl = keyframeUrl;
    }

    public LiveStatus getStatus() {
        return status;
    }

    public void setStatus(LiveStatus status) {
        this.status = status;
    }

    public Long getLiveTime() {
        return liveTime;
    }

    public void setLiveTime(Long liveTime) {
        this.liveTime = liveTime;
    }

    public Integer getOnline() {
        return online;
    }

    public void setOnline(Integer online) {
        this.online = online;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getAreaName() {
        return areaName;
    }

    public void setAreaName(String areaName) {
        this.areaName = areaName;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    @Override
    public String toString() {
        return "LiveRoom{" +
                "roomName='" + roomName + '\'' +
                ", userName='" + userName + '\'' +
                ", fromUrl='" + fromUrl + '\'' +
                ", keyframeUrl='" + keyframeUrl + '\'' +
                ", status=" + status +
                ", uid=" + uid +
                ", liveTime=" + liveTime +
                ", online=" + online +
                ", tags='" + tags + '\'' +
                ", areaName='" + areaName + '\'' +
                '}';
    }
}
