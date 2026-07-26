package com.now.nowbot.entity.bind

import com.now.nowbot.entity.SBBindUserLite
import com.now.nowbot.entity.SBBindUserLite.Companion.toModel
import com.now.nowbot.model.SBBindUser
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.OneToOne
import jakarta.persistence.Table

@Entity
@Table(name = "sb_bind_qq")
data class SBQQBindLite(
    @Id
    var qq: Long,

    @OneToOne(targetEntity = SBBindUserLite::class, orphanRemoval = true)
    var bindUserLite: SBBindUserLite,
) {
    val bindUser: SBBindUser
        get() = bindUserLite.toModel()

    interface QQUser {
        val qid: Long
        val uid: Long
        val name: String
    }
}
