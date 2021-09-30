package com.now.nowbot.entity;

import javax.persistence.*;

@Entity
@Table(indexes = {@Index(name = "sid", columnList = "map_id")})
public class BitmapLite {
    @Id
    @Column(name = "id")
    Integer bitmapID;

    @Column(name = "map_id")
    Integer MapSetId;

    public Integer getBitmapID() {
        return bitmapID;
    }

    public void setBitmapID(Integer bitmapID) {
        this.bitmapID = bitmapID;
    }

    public Integer getMapSetId() {
        return MapSetId;
    }

    public void setMapSetId(Integer mapSetId) {
        MapSetId = mapSetId;
    }
}
