package com.now.nowbot.model.jsonData;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BeatMapSetDiscussionPost {
    @JsonProperty("beatmapset_discussion_id")
    Long DID;

    @JsonProperty("created_at")
    OffsetDateTime createdAt;

    @JsonProperty("deleted_at")
    OffsetDateTime deletedAt;

    @JsonProperty("deleted_by_id")
    Long deletedByUID;

    @JsonProperty("id")
    Long PID;

    @JsonProperty("last_editor_id")
    Long lastEditorID;

    String message;

    Boolean system;

    @JsonProperty("updated_at")
    OffsetDateTime updatedAt;

    @JsonProperty("user_id")
    Long UID;

    public Long getDID() {
        return DID;
    }

    public void setDID(Long DID) {
        this.DID = DID;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }


    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(OffsetDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }


    public Long getDeletedByUID() {
        return deletedByUID;
    }

    public void setDeletedByUID(Long deletedByUID) {
        this.deletedByUID = deletedByUID;
    }

    public Long getPID() {
        return PID;
    }

    public void setPID(Long PID) {
        this.PID = PID;
    }


    public Long getLastEditorID() {
        return lastEditorID;
    }

    public void setLastEditorID(Long lastEditorID) {
        this.lastEditorID = lastEditorID;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Boolean getSystem() {
        return system;
    }

    public void setSystem(Boolean system) {
        this.system = system;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getUID() {
        return UID;
    }

    public void setUID(Long UID) {
        this.UID = UID;
    }
}
