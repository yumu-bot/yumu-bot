package com.now.nowbot.qq.onebot.contact

import com.mikuac.shiro.core.Bot
import com.mikuac.shiro.dto.action.common.ActionData
import com.mikuac.shiro.dto.action.response.DownloadFileResp
import com.mikuac.shiro.dto.action.response.GroupMemberInfoResp
import com.now.nowbot.qq.contact.Contact.Companion.log
import com.now.nowbot.qq.contact.Group
import com.now.nowbot.util.QQMsgUtil.botInLocal
import com.now.nowbot.util.QQMsgUtil.byte2str
import com.now.nowbot.util.QQMsgUtil.getFilePubUrl
import com.now.nowbot.util.QQMsgUtil.getFileUrl
import com.now.nowbot.util.QQMsgUtil.removeFileUrl

class Group : Contact, Group {
    override var name: String? = null
    get() = field ?: try {
        val data = bot.getGroupInfo(contactID, false).getData()
        data.groupName
    } catch (_: Exception) {
        log.error("获取群名 $contactID 失败")
        "unknown group"
    }

    constructor(bot: Bot, groupID: Long) : super(bot, groupID)

    constructor(bot: Bot, groupID: Long, name: String?) : super(bot, groupID) {
        this.name = name
    }

    override val isAdmin: Boolean
        get() {
            val data = bot.getGroupMemberInfo(contactID, bot.selfId, false).getData()
            return data.role == "owner" || data.role == "admin"
        }

    override fun getUser(qq: Long): com.now.nowbot.qq.contact.GroupContact {
        val data = bot.getGroupMemberInfo(contactID, qq, false).getData()
        return GroupContact(bot, data.userId, this.contactID, data.nickname, data.role)
    }

    override val allUser: List<GroupContact>
        get() {
            val data = bot.getGroupMemberList(contactID).getData()
            return data.mapNotNull { f: GroupMemberInfoResp? ->
                f?.let {
                    GroupContact(
                        bot,
                        f.userId,
                        this.contactID,
                        f.nickname,
                        f.role
                    )
                }
            }
        }

    override fun sendFile(data: ByteArray, name: String) {
        val url = if (botInLocal(bot.selfId)) {
            getFileUrl(data, name)
        } else {
            getFilePubUrl(data, name)
        }

        try {
            var rep: ActionData<DownloadFileResp?>? = null

            repeat(5) { // 执行 0 到 4，共 5 次
                if (rep != null) return@repeat // 如果已经成功，跳过后续尝试

                rep = bot.customRawRequest(
                    { "download_file" },
                    mapOf(
                        "name" to name,
                        "base64" to byte2str(data)
                    ),
                    DownloadFileResp::class.java
                )
            }

            if (rep == null || rep.data == null) {
                rep = bot.downloadFile(url)
            }

            if (rep.data == null) {
                log.error("发送文件失败: 客户端不支持接收文件")
                return
            }

            bot.uploadGroupFile(contactID, rep.data!!.file, name)
        } catch (e: Exception) {
            log.error("文件上传错误", e)
        } finally {
            removeFileUrl(url)
        }
    }
}
