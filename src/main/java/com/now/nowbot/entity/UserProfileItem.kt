package com.now.nowbot.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import java.io.Serializable

@Entity
@IdClass(UserProfileKey::class)
class UserProfileItem(
    @Id
    var userId: Long,

    @Id
    @Column(name = "type", columnDefinition = "TEXT")
    var type: String,

    @Column(name = "path", columnDefinition = "TEXT")
    var path: String? = null,

    var verify:Boolean = false,
)


data class UserProfileKey(
    var userId: Long,
    var type: String,
) : Serializable {
    constructor() : this(0, "")
}