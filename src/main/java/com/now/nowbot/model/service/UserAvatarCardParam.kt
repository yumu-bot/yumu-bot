package com.now.nowbot.model.service

import com.now.nowbot.model.json.OsuUser


data class UserAvatarCardParam(
    var banner: String? = null,
    var avatar: String? = null,
    var color: String? = null,
    var name: String? = null,
) {
    constructor(user: OsuUser): this(
        user.coverUrl,
        user.avatarUrl,
        "hsl(${user.profileHue},60%,50%)",
        user.username,
    )
}
