package com.now.nowbot.entity.bind

import com.now.nowbot.dao.BindDao.Companion.fromLite
import com.now.nowbot.entity.OsuBindUserLite
import com.now.nowbot.model.BindUser
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.OneToOne
import jakarta.persistence.Table

@Entity @Table(name = "osu_bind_qq")
class QQBindLite {
    @Id
    var qq: Long? = null

    @OneToOne(targetEntity = OsuBindUserLite::class, orphanRemoval = true)
    var osuUser: OsuBindUserLite? = null

    val bindUser: BindUser?
        get() = fromLite(osuUser)

    interface QQUser {
        val qid: Long
        val uid: Long
        val name: String?
    }
}
