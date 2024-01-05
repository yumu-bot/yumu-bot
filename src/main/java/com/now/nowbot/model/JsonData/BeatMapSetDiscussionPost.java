package com.now.nowbot.model.JsonData;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.lang.Nullable;

import java.time.OffsetDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BeatMapSetDiscussionPost {
    @JsonProperty("beatmapset_discussion_id")
    Long DID;

    @JsonProperty("created_at")
    OffsetDateTime createdAt;

    @JsonProperty("deleted_at")
    @Nullable
    OffsetDateTime deletedAt;

    @JsonProperty("deleted_by_id")
    @Nullable
    Long deletedByUID;

    @JsonProperty("id")
    Long PID;

    @JsonProperty("last_editor_id")
    @Nullable
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

    @Nullable
    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(@Nullable OffsetDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    @Nullable
    public Long getDeletedByUID() {
        return deletedByUID;
    }

    public void setDeletedByUID(@Nullable Long deletedByUID) {
        this.deletedByUID = deletedByUID;
    }

    public Long getPID() {
        return PID;
    }

    public void setPID(Long PID) {
        this.PID = PID;
    }

    @Nullable
    public Long getLastEditorID() {
        return lastEditorID;
    }

    public void setLastEditorID(@Nullable Long lastEditorID) {
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
