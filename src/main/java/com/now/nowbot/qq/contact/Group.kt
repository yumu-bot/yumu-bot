package com.now.nowbot.qq.contact

interface Group : Contact {
    override val name: String?

    val isAdmin: Boolean

    val allUser: List<GroupContact>

    fun getUser(qq: Long): GroupContact

    fun sendFile(data: ByteArray, name: String)
}
