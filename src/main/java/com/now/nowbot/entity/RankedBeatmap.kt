package com.now.nowbot.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import org.hibernate.annotations.Subselect

@Entity(name = "osu_ranked_beatmap_id")
@Subselect("""
    select id as bid from osu_beatmap ob 
    left join osu_mapset om on ob.map_id = om.map_id
    where ob.status='ranked' or ob.status='approved';
    """)
class RankedBeatmap(
    @Id
    @Column(name = "bid")
    var bid: Long
)