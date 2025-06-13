package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.bili.BiliUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.biliApiService.BiliApiService
import com.now.nowbot.throwable.botRuntimeException.IllegalArgumentException
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.FLAG_ID
import org.springframework.stereotype.Service

@Service("BILI_USER")
class BiliUserService(private val biliApiService: BiliApiService): MessageService<Long> {
    override fun isHandle(event: MessageEvent, messageText: String, data: MessageService.DataValue<Long>): Boolean {
        val matcher = Instruction.BILI_USER.matcher(messageText)

        if (! matcher.find()) return false

        val userID: Long = matcher.group(FLAG_ID)?.toLongOrNull() ?: throw IllegalArgumentException.WrongException.PlayerID()

        data.value = userID

        return true
    }

    override fun HandleMessage(event: MessageEvent, param: Long) {
        val user = biliApiService.getUser(param)

        event.reply(getMessage(user.data!!, biliApiService))
    }

    companion object {
        private fun getMessage(user: BiliUser.UserData, biliApiService: BiliApiService) : MessageChain {

            val mb = MessageChain.MessageChainBuilder()

            val sex = when (user.sex) {
                "男" -> "♂"
                "女" -> "♀"
                else -> "?"
            }

            val senior = if (user.seniorMember > 0) {
                "+"
            } else {
                ""
            }

            val sb = StringBuilder("""
                
                ${user.name} ($sex)
                ${user.signature}
                Lv.${user.level}${senior} ${user.vip?.label?.text ?: ""}
                LiveRoom: ${user.liveRoom.id} ${if (user.liveRoom.liveStatus == 1.toByte()) "正在直播！" else ""} ${user.liveRoom.watched.text}
                
                birthday: ${user.birthday}
                follower_medal:
                
            """.trimIndent())

            if (user.birthday != null) {
                sb.append("\nbirthday: ${user.birthday}")
            }

            if (user.fansMetal.medal != null) {
                val m = user.fansMetal.medal

                sb.append("\nmedal: ${m.name} Lv.${m.level} (${m.intimacy})")
            }

            if (user.birthday != null) {
                sb.append("\nbirthday: ${user.birthday}")
            }

            if (user.school.name.isNotBlank()) {
                sb.append("\nschool: ${user.school.name}")
            }

            if (user.profession.name.isNotBlank()) {
                sb.append("\nprofession: ${user.profession.name} (${user.profession.department})")
            }

            if (user.tags != null) {
                sb.append("\ntags: ${user.tags.joinToString()}")
            }

            val avatar = biliApiService.getImage(user.avatar)
            mb.addImage(avatar).addText(sb.toString())

            return mb.build()
        }
    }
}