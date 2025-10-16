package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.dao.BindDao
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.model.osu.OsuUser.Companion.toMicroUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.OldAvatarService.OAParam
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.throwable.botRuntimeException.NetworkException
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.util.AsyncMethodExecutor
import com.now.nowbot.util.DataUtil
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.OfficialInstruction
import com.now.nowbot.util.QQMsgUtil
import com.now.nowbot.util.command.FLAG_DATA
import com.now.nowbot.util.command.FLAG_MODE
import com.now.nowbot.util.command.FLAG_QQ_ID
import com.now.nowbot.util.command.FLAG_UID
import com.now.nowbot.util.command.REG_SEPERATOR_NO_SPACE
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.concurrent.ExecutionException
import java.util.regex.Matcher

@Service("OLD_AVATAR")
class OldAvatarService(
    private val userApiService: OsuUserApiService,
    private val beatmapApiService: OsuBeatmapApiService,
    private val bindDao: BindDao,
    private val imageService: ImageService,
) : MessageService<OAParam>, TencentMessageService<OAParam> {

    data class OAParam(
        val qq: Long?,
        val uid: Long?,
        val name: String?,
        val at: Boolean,
        val isMyself: Boolean,
        val mode: OsuMode,
        val version: Int = 1,
    )

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: DataValue<OAParam>,
    ): Boolean {
        val m1 = Instruction.OLD_AVATAR.matcher(messageText)
        val m2 = Instruction.OLD_AVATAR_CARD.matcher(messageText)

        val matcher: Matcher
        val version: Int

        if (m1.find()) {
            matcher = m1
            version = 1
        } else if (m2.find()) {
            matcher = m2
            version = 2
        } else return false

        data.value = getParam(event, matcher, version)
        return true
    }

    @Throws(Throwable::class)
    override fun handleMessage(event: MessageEvent, param: OAParam): ServiceCallStatistic? {
        val users = getUsers(param)
        val images = getImages(users, param.version)

        try {
            if (images.size == 1) {
                event.reply(images.first())
            } else {
                QQMsgUtil.sendImages(event, images)
            }
        } catch (e: Exception) {
            log.error("旧头像：发送失败", e)
            throw IllegalStateException.Send("官网头像")
        }

        return ServiceCallStatistic.builds(event, userIDs = users.map { it.userID })
    }

    override fun accept(event: MessageEvent, messageText: String): OAParam? {
        val m1 = OfficialInstruction.OLD_AVATAR.matcher(messageText)
        val m2 = Instruction.OLD_AVATAR_CARD.matcher(messageText)

        val matcher: Matcher
        val version: Int

        if (m1.find()) {
            matcher = m1
            version = 1
        } else if (m2.find()) {
            matcher = m2
            version = 2
        } else return null

        return getParam(event, matcher, version)
    }

    override fun reply(event: MessageEvent, param: OAParam): MessageChain? {
        val users = getUsers(param)
        val images = getImages(users)

        try {
            return if (images.size == 1) {
                MessageChain(images.first())
            } else {
                // 官方 QQ 消息限制：只能发一张图
                QQMsgUtil.getImages(images).first()
            }
        } catch (e: Exception) {
            log.error("旧头像：发送失败", e)
            throw IllegalStateException.Send("官网头像")
        }
    }

    private fun getParam(event: MessageEvent, matcher: Matcher, version: Int = 1): OAParam {
        val qqStr: String = matcher.group(FLAG_QQ_ID) ?: ""
        val uidStr: String = matcher.group(FLAG_UID) ?: ""
        val name: String = matcher.group(FLAG_DATA) ?: ""
        val mode = OsuMode.getMode(matcher.group(FLAG_MODE), bindDao.getGroupModeConfig(event))

        return if (event.hasAt()) {
            OAParam(event.target, null, null, at = true, isMyself = false, mode = mode, version = version)
        } else if (qqStr.isNotBlank()) {
            OAParam(qqStr.toLongOrNull(), null, null, at = false, isMyself = false, mode = mode, version = version)
        } else if (uidStr.isNotBlank()) {
            OAParam(null, uidStr.toLongOrNull(), null, at = false, isMyself = false, mode = mode, version = version)
        } else if (name.isNotBlank()) {
            OAParam(null, null, name.trim(), at = false, isMyself = false, mode = mode, version = version)
        } else {
            OAParam(event.sender.id, null, null, at = false, isMyself = true, mode = mode, version = version)
        }
    }


    private fun parseDataString(dataStr: String?, mode: OsuMode): List<OsuUser> {
        if (dataStr.isNullOrBlank()) return emptyList()

        val strings =
            dataStr
                .trim { it <= ' ' }
                .split(REG_SEPERATOR_NO_SPACE.toRegex())
                .dropLastWhile { it.isEmpty() }
                .map { it.trim() }
                .dropWhile { it.isBlank() }
                .distinct()

        return if (strings.size == 1) {
            try {
                listOf(userApiService.getOsuUser(strings.first(), mode))
            } catch (_: Exception) {
                listOf(getBannedPlayerFromSearch(strings.first(), mode))
            }
        } else {
            try {
                AsyncMethodExecutor.awaitCallableExecute({
                    strings.map { name ->
                        userApiService.getOsuUser(name, mode)
                    }
                })
            } catch (_: ExecutionException) {
                // 可能含有被 ban 的玩家，退回到逐个获取
                strings.map { name ->
                    try {
                        userApiService.getOsuUser(name, mode)
                    } catch (_: NetworkException) {
                        getBannedPlayerFromSearch(name, mode)
                    }
                }
            }
        }
    }

    private fun getBannedPlayerFromSearch(input: String, mode: OsuMode): OsuUser {

        // 如果输入的格式是 deleteduser_121313 的形式，那么后面的数字就是玩家 id

        val matcher = "DeletedUser_(\\d+)".toPattern().matcher(input.trim())

        val isDeleted = matcher.find()

        val name = if (isDeleted) {
            matcher.group(1)
        } else {
            input.trim()
        }

        // 构建谱面查询，获取被封禁玩家的 id
        val query = mapOf(
            "q" to name, "s" to "any", "page" to 1
        )

        val search = beatmapApiService.searchBeatmapset(query)

        val pairs = search.beatmapsets
            .sortedByDescending { it.lastUpdated?.toInstant()?.toEpochMilli() ?: 0L }
            .sortedByDescending { it.ranked }
            .filter {
                if (isDeleted) {
                    DataUtil.getStringSimilarity(it.creatorID.toString(), name) > 0.8
                } else {
                    DataUtil.getStringSimilarity(it.creator, name) > 0.8
                }
            }
            .map { it.creatorID to it.beatmapsetID }

        // 找到重复最多次的键
        val userID = pairs
            .groupingBy { it.first }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key

        // 找到该键的第一个值
        val setID = pairs
            .firstOrNull { it.first == userID }
            ?.second ?: throw NoSuchElementException.Player(name)

        val set = beatmapApiService.getBeatmapset(setID)

        if (set.creatorData == null) {
            throw NoSuchElementException.Player(name)
        }

        val m = OsuMode.getMode(mode, OsuMode.getMode(set.ranked))

        val banned = set.creatorData!!.apply {
            this.username = set.creator
            this.currentOsuMode = m
        }

        return banned
    }

    private fun getUsers(param: OAParam): List<OsuUser> {
        val user: OsuUser

        if (param.uid != null) {
            try {
                user = userApiService.getOsuUser(param.uid, param.mode)
            } catch (_: Exception) {
                throw NoSuchElementException.Player(param.uid.toString())
            }
        } else if (param.qq != null) {
            val bind = bindDao.getBindFromQQ(param.qq)

            user = userApiService.getOsuUser(bind.userID, OsuMode.getMode(param.mode, bind.mode))
        } else {
            val users = parseDataString(param.name, param.mode)

            if (users.isEmpty()) {
                throw IllegalStateException.Fetch("玩家名")
            }

            userApiService.asyncDownloadAvatar(users.map { it.toMicroUser() })

            return users
        }

        return listOf(user)
    }

    private fun getImages(users: List<OsuUser>, version: Int = 1): List<ByteArray> {
        val panel = when (version) {
            2 -> "Epsilon2"
            else -> "Epsilon"
        }

        return if (users.size > 1) {
            try {
                AsyncMethodExecutor.awaitSupplierExecute(
                    users.map { u ->
                        AsyncMethodExecutor.Supplier {
                            imageService.getPanel(mapOf("user" to u), panel)
                        }
                    }, Duration.ofSeconds(30L + users.size / 2)
                )
            } catch (_: ExecutionException) {
                throw NetworkException.RenderModuleException.BadGateway()
            }
        } else {
            listOf(imageService.getPanel(mapOf("user" to users.first()), panel))
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(OldAvatarService::class.java)
    }
}
