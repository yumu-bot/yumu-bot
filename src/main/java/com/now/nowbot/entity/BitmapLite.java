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
}
