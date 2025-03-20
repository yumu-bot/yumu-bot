package com.now.nowbot.entity

import com.now.nowbot.model.enums.OsuMode
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "osu_group_config")
class OsuGroupConfigLite (
    @Id
    @Column(name = "group_id")
    var groupId: Long? = null,
    @Column(name = "main_mode")
    var mainMode: OsuMode? = null
){
}