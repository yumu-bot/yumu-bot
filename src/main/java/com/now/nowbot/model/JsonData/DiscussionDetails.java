package com.now.nowbot.model.JsonData;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.lang.Nullable;

import java.time.OffsetDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DiscussionDetails {
    @JsonProperty("id")
    Long DID;

    @JsonProperty("beatmapset_id")
    Long SID;

    @JsonProperty("beatmap_id")
    @Nullable
    Long BID;

    @JsonProperty("user_id")
    Long UID;

    @JsonProperty("deleted_by_id")
    @Nullable
    Long deletedByUID;

    //hype, mapper_note, problem, suggestion, praise, review. Blank defaults to all types
    @JsonProperty("message_type")
    MessageType messageType;

    @JsonProperty("parent_id")
    Long parentDID;

    Long timeStamp;

    Boolean resolved;

    @JsonProperty("can_be_resolved")
    Boolean canBeResolved;

    @JsonProperty("can_grant_kudosu")
    Boolean canGrantKudosu;

    @JsonProperty("created_at")
    OffsetDateTime createdAt;

    @JsonProperty("updated_at")
    OffsetDateTime updatedAt;

    @JsonProperty("deleted_at")
    OffsetDateTime deletedAt;

    @JsonProperty("last_post_at")
    OffsetDateTime lastPostAt;

    @JsonProperty("kudosu_denied")
    Boolean kudosuDenied;

    @JsonProperty("starting_post")
    BeatMapSetDiscussionPost post;

    public Long getDID() {
        return DID;
    }

    public void setDID(Long DID) {
        this.DID = DID;
    }

    public Long getSID() {
        return SID;
    }

    public void setSID(Long SID) {
        this.SID = SID;
    }

    @Nullable
    public Long getBID() {
        return BID;
    }

    public void setBID(@Nullable Long BID) {
        this.BID = BID;
    }

    public Long getUID() {
        return UID;
    }

    public void setUID(Long UID) {
        this.UID = UID;
    }

    @Nullable
    public Long getDeletedByUID() {
        return deletedByUID;
    }

    public void setDeletedByUID(@Nullable Long deletedByUID) {
        this.deletedByUID = deletedByUID;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public Long getParentDID() {
        return parentDID;
    }

    public void setParentDID(Long parentDID) {
        this.parentDID = parentDID;
    }

    public Long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(Long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public Boolean getResolved() {
        return resolved;
    }

    public void setResolved(Boolean resolved) {
        this.resolved = resolved;
    }

    public Boolean getCanBeResolved() {
        return canBeResolved;
    }

    public void setCanBeResolved(Boolean canBeResolved) {
        this.canBeResolved = canBeResolved;
    }

    public Boolean getCanGrantKudosu() {
        return canGrantKudosu;
    }

    public void setCanGrantKudosu(Boolean canGrantKudosu) {
        this.canGrantKudosu = canGrantKudosu;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(OffsetDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public OffsetDateTime getLastPostAt() {
        return lastPostAt;
    }

    public void setLastPostAt(OffsetDateTime lastPostAt) {
        this.lastPostAt = lastPostAt;
    }

    public Boolean getKudosuDenied() {
        return kudosuDenied;
    }

    public void setKudosuDenied(Boolean kudosuDenied) {
        this.kudosuDenied = kudosuDenied;
    }

    public BeatMapSetDiscussionPost getPost() {
        return post;
    }

    public void setPost(BeatMapSetDiscussionPost post) {
        this.post = post;
    }

    public enum MessageType {
        hype,
        mapper_note,
        problem,
        suggestion,
        praise,
        review,
    }
}
