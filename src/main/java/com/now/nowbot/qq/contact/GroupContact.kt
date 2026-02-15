package com.now.nowbot.qq.contact

import com.now.nowbot.qq.enums.Role

interface GroupContact : Friend {
    val role: Role?
}
