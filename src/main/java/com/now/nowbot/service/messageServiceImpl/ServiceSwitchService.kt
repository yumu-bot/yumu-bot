package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.config.Permission
import com.now.nowbot.dao.BindDao
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.permission.PermissionController
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.ServiceSwitchService.Operate.*
import com.now.nowbot.service.messageServiceImpl.ServiceSwitchService.SwitchParam
import com.now.nowbot.throwable.TipsException
import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.throwable.botRuntimeException.PermissionException
import com.now.nowbot.util.ASyncMessageUtil
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.FLAG_NAME
import com.now.nowbot.util.command.FLAG_QQ_GROUP
import com.now.nowbot.util.command.FLAG_QQ_ID
import com.now.nowbot.util.command.FLAG_SERVICE
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.regex.Matcher

@Service("SWITCH")
class ServiceSwitchService(
    val controller: PermissionController,
    val bindDao: BindDao,
    val imageService: ImageService,
) : MessageService<SwitchParam> {

    enum class ServiceType(private val typeAlias: List<String>, val services: List<String>) {
        BOT(listOf("b", "bot", "内部", "机器"),
            listOf("help", "ping", "bind", "switch", "service_count", "login", "echo", "check", "update", "fetch", "refresh_file")
        ),

        SCORE(listOf("s", "scores", "成绩", "分数"),
            listOf("set_mode", "sb_set_mode", "set_group_mode", "score_pr", "sb_score_pr", "pr_card", "uu_pr", "score", "sb_score", "uu_score", "bp", "sb_bp", "today_bp", "sb_today_bp", "bp_analysis", "uu_ba", "bp_analysis_legacy", "bp_fix", "top_plays", "recent_best")
        ),

        PLAYER(listOf("p", "player", "players", "user", "users", "玩家", "用户"),
            listOf("info", "info_card", "uu_info", "i_mapper", "friend", "mutual", "skill", "pp_minus", "team", "badge", "guest_difficulty", "get_id", "get_name", "pp_plus")
        ),

        BEATMAP(listOf("b", "beatmap", "maps", "beatmaps", "谱面", "图"),
            listOf("map", "audio", "explore", "qualified_map", "nomination", "map_minus", "leader_board", "get_cover", "refresh_file")
        ),

        MATCH(listOf("m", "match", "tour", "tournament", "matches", "multiplayer", "mp", "比赛", "房间"),
            listOf("mu_rating", "series_rating", "match_listener", "match_now", "match_recent", "match_round", "get_pool")
        ),

        CHAT(listOf("c", "chat", "chats", "聊天"),
            listOf()
        ),

        FUN(listOf("f", "fun", "amuse", "amusement", "娱乐", "玩"),
            listOf("dice")
        ),

        MAIMAI(listOf("i", "mai", "maimai", "chu", "chunithm", "舞萌", "舞萌中二", "中二", "中二节奏"),
            listOf("mai_audio", "mai_bp", "mai_score", "mai_dist", "mai_find", "mai_version", "mai_filter", "chu_bp", "mai_seek")
        ),

        AID(listOf("a", "aid", "other", "辅助"),
            listOf("old_avatar", "sb_old_avatar", "take", "newbie_restrict", "trans", "kita")
        ),

        CUSTOM(listOf("u", "customize", "自定"),
            listOf("custom")
        ),

        PRIVATE(listOf("e", "private", "私服"),
            listOf("sb_set_mode", "sb_info", "sb_score_pr", "sb_score", "sb_bp", "sb_today_bp")
        ),

        ;

        companion object {
            val typeMap = ServiceType.entries.associateWith { it.typeAlias }

            fun getTypeFromInput(input: String): ServiceType? {
                val standard = input.lowercase()

                typeMap.forEach { (type, typeAlias) ->
                    if (typeAlias.contains(standard)) {
                        return type
                    }
                }

                return null
            }
        }
    }

    private val full = controller.queryAllBlock().map { it.name }

    enum class Operate {
        ON, OFF, LIST
    }

    enum class Target {
        QQ, GROUP
    }

    enum class Level {
        SUPER, GROUP, USER
    }

    data class SwitchParam(
        val services: List<String>,
        val operate: Operate,
        val target: Target?,
        val id: Long?
    )

    private fun getLevel(event: MessageEvent): Level {
        return if (Permission.isSuperAdmin(event)) {
            Level.SUPER
        } else if (Permission.isGroupAdmin(event)) {
            Level.GROUP
        } else Level.USER
    }

    private fun getParam(event: MessageEvent, matcher: Matcher, operate: Operate): SwitchParam {
        val level = getLevel(event)

        if (operate == LIST && level != Level.SUPER) {
            throw PermissionException.DeniedException.BelowSuperAdministrator()
        }

        val target = getTarget(event, matcher, level)

        val targetStr = if (target == null) {
            "所有人"
        } else {
            when (target.first) {
                Target.QQ -> "qq 用户："
                Target.GROUP -> "qq 群组："
            } + target.second
        }

        val services = getServices(event, targetStr, matcher.group(FLAG_SERVICE), operate)

        if (operate == LIST) {
            return SwitchParam(services, LIST, target?.first, target?.second)
        }

        return SwitchParam(services, operate, target?.first, target?.second)
    }

    private fun getTarget(event: MessageEvent, matcher: Matcher, level: Level): Pair<Target, Long>? {
        val qq: Long? = if (event.hasAt()) {
            event.target
        } else {
            matcher.group(FLAG_QQ_ID)?.toLongOrNull()
        }

        val group: Long? = matcher.group(FLAG_QQ_GROUP)?.toLongOrNull()

        val name: String = matcher.group(FLAG_NAME) ?: ""

        if (level == Level.SUPER) {
            if (qq != null) {
                return Target.QQ to qq
            } else if (group != null) {
                return Target.GROUP to group
            } else if (name.isNotBlank()) {
                val user = bindDao.getBindUser(name.trim()) ?: throw TipsException("""
                对方没有绑定。请使用 qq= 来确定对方的 QQ。
            """.trimIndent())

                return Target.QQ to bindDao.getQQ(user)
            } else {
                return null
            }
        } else {
            if (qq != null && qq != event.sender.id) {
                throw TipsException("""
                    权限不足！只有超级管理员拥有控制其他 QQ 用户的权限。
                """.trimIndent())
            } else if (group != null && group != event.subject.id) {
                throw TipsException("""
                    权限不足！只有超级管理员拥有控制其他 QQ 群聊的权限。
                """.trimIndent())
            } else if (name.isNotBlank()) {
                throw TipsException("""
                    权限不足！只有超级管理员拥有控制其他 osu! 用户的权限。
                """.trimIndent())
            } else {
                // 选择操作模式

                if (level == Level.USER) {
                    // 普通用户只能控制自己
                    return Target.QQ to event.sender.id
                } else {
                    if (qq == event.sender.id) {
                        return Target.QQ to qq
                    } else if (group == event.subject.id) {
                        return Target.GROUP to group
                    }

                    // 这里必定是群聊管理员
                    // 群聊管理员可以控制群聊的开关，所以在模棱两可的时候，需要询问。

                    val receipt = event.reply("""
                        您需要操作群聊还是操作您自己呢？
                        回复 1 操作群聊
                        回复 2 操作自己
                        """.trimIndent())

                    val lock = ASyncMessageUtil.getLock(event, 30 * 1000L)

                    val ev = lock.get()
                    receipt.recall()

                    return if (ev != null && ev.rawMessage.contains("1", ignoreCase = true)) {
                        Target.GROUP to event.subject.id
                    } else if (ev != null && ev.rawMessage.contains("2", ignoreCase = true)) {
                        Target.QQ to event.sender.id
                    } else if (ev == null) {
                        throw TipsException("确认超时。")
                    } else {
                        throw TipsException("操作已中止。")
                    }

                }
            }
        }
    }

    private fun getServices(event: MessageEvent, target: String, input: String?, operate: Operate): List<String> {
        if (operate == LIST) return listOf()

        val ii = (input ?: "").trim().replace("\\s+".toRegex(), "_").uppercase()

        val type = ServiceType.getTypeFromInput(ii)

        val does = when(operate) {
            OFF -> "关闭"
            ON -> "开启"
            else -> "查看"
        }

        if (ii.isEmpty()) {
            // 全局操作模式

            val receipt = event.reply("""
                您确定要${does}所有服务吗？回复 OK 确认。
                操作对象：${target}
                操作服务：所有
                
                如果不想${does}所有服务，也可以尝试按类别来${does}。
            """.trimIndent())

            val lock = ASyncMessageUtil.getLock(event, 30 * 1000L)

            val ev = lock.get()
            receipt.recall()

            if (ev != null && ev.rawMessage.contains("OK", ignoreCase = true)) {
                return listOf()
            } else if (ev == null) {
                throw TipsException("确认超时。")
            } else {
                throw TipsException("${does}操作已中止。")
            }
        }

        if (type == null) {
            val service = try {
                controller.queryBlock(ii)
            } catch (_: RuntimeException) {
                null
            }

            if (service != null) {
                // 单服务操作模式
                return listOf(service.name)
            }

            // 重复确认模式
            val confirm = StringBuilder("""
                没有找到服务 ${ii}。
                您可以按类别来操作服务。
                
                可用的类别：
                
            """.trimIndent())

            confirm.append(ServiceType.entries.joinToString("\n") {
                "${it.name}:\n${it.services.joinToString(", ")}"
            })

            throw TipsException(confirm.toString())

        } else {
            // 种类操作模式
            val receipt = event.reply("""
                您确定要${does} ${type.name} 类别下的所有服务吗？回复 OK 确认。
                操作对象：${target}
                操作服务：${type.services.joinToString(", ")}
            """.trimIndent())

            val lock = ASyncMessageUtil.getLock(event, 30 * 1000L)

            val ev = lock.get()
            receipt.recall()

            if (ev != null && ev.rawMessage.contains("OK", ignoreCase = true)) {
                return type.services.map { it.uppercase() }
            } else if (ev == null) {
                throw TipsException("确认超时。")
            } else {
                throw TipsException("${does}操作已中止。")
            }
        }
    }


    private fun SwitchParam.handle(): MessageChain {

        return when(this.operate) {
            ON -> when(this.target) {
                Target.QQ -> if (this.services.isEmpty()) {
                    controller.clearUser(this.id!!)

                    MessageChain("操作已完成：开启用户 ${this.id} 的所有服务")
                } else {
                    this.services.forEach { serv ->
                        controller.unblockUser(serv, this.id!!)
                    }

                    MessageChain("操作已完成：开启用户 ${this.id} 的 ${services.joinToString(", ")} 服务")
                }
                Target.GROUP -> if (this.services.isEmpty()) {
                    controller.clearGroup(this.id!!)

                    MessageChain("操作已完成：开启群聊 ${this.id} 的所有服务")
                } else {
                    this.services.forEach { serv ->
                        controller.unblockGroup(serv, this.id!!)
                    }

                    MessageChain("操作已完成：开启群聊 ${this.id} 的 ${services.joinToString(", ")} 服务")
                }
                null -> if (this.services.isEmpty()) {
                    full.forEach { serv ->
                        controller.serviceSwitch(serv, true)
                    }

                    MessageChain("操作已完成：开启所有服务")
                } else {
                    this.services.forEach { serv ->
                        controller.serviceSwitch(serv, true)
                    }

                    MessageChain("操作已完成：开启 ${services.joinToString(", ")} 服务")
                }
            }
            OFF -> when(this.target) {
                Target.QQ -> if (this.services.isEmpty()) {
                    controller.blockUser(this.id!!)

                    MessageChain("操作已完成：关闭用户 ${this.id} 的所有服务")
                } else {
                    this.services.forEach { serv ->
                        controller.blockUser(serv, this.id!!)
                    }

                    MessageChain("操作已完成：关闭用户 ${this.id} 的 ${services.joinToString(", ")} 服务")
                }
                Target.GROUP -> if (this.services.isEmpty()) {
                    controller.blockGroup(this.id!!)

                    MessageChain("操作已完成：关闭群聊 ${this.id} 的所有服务")
                } else {
                    this.services.forEach { serv ->
                        controller.blockGroup(serv, this.id!!)
                    }

                    MessageChain("操作已完成：关闭群聊 ${this.id} 的 ${services.joinToString(", ")} 服务")
                }
                null -> if (this.services.isEmpty()) {
                    full.forEach { serv ->
                        controller.serviceSwitch(serv, false)
                    }

                    MessageChain("操作已完成：关闭所有服务")
                } else {
                    this.services.forEach { serv ->
                        controller.serviceSwitch(serv, false)
                    }

                    MessageChain("操作已完成：关闭 ${services.joinToString(", ")} 服务")
                }
            }

            LIST -> {
                val global = controller.queryGlobal()
                val all = controller.queryAllBlock()

                val filtered = when (this.target) {
                    Target.QQ -> all.filter { it.enable && it.users.isNotEmpty() }.sortedByDescending { it.users.size }
                    Target.GROUP -> all.filter { it.enable && it.groups.isNotEmpty() }.sortedByDescending { it.groups.size }
                    null -> all.sortedBy { it.name }.sortedBy { it.enable }
                }

                val markdown = getStatisticsList(filtered, global)

                val image = try {
                    imageService.getPanelA6(markdown, "switch")
                } catch (e: Exception) {
                    log.error("服务控制：渲染失败", e)
                    throw IllegalStateException.Render("服务控制")
                }

                MessageChain(image)
            }
        }
    }

    private fun getStatisticsList(list: List<PermissionController.LockRecord>, global: PermissionController.LockRecord): String {
        val sb = StringBuilder()

        sb.append("## 服务列表\n")

        sb.append("""
            
            | 状态 | 服务名 | 无法使用的群聊 | 无法使用的用户
            | :-: | :-- | :-- | :-- |
            
            """.trimIndent()
        )

        val gs = global.groups.take(5).joinToString(", ")
        val us = global.users.take(5).joinToString(", ")

        sb.append("| ")
            .append("*")
            .append(" | ")
            .append("GLOBAL")
            .append(" | ")
            .append(gs)
            .append(" | ")
            .append(us)
            .append(" |\n")

        list.forEach {
            val gss = it.groups.take(5).joinToString(", ")
            val uss = it.users.take(5).joinToString(", ")

            sb.append("| ")
                .append(if (it.enable) "ON" else "OFF")
                .append(" | ")
                .append(it.name)
                .append(" | ")
                .append(gss)
                .append(" | ")
                .append(uss)
                .append(" |\n")
        }

        return sb.toString()
    }

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: DataValue<SwitchParam>
    ): Boolean {
        val m = Instruction.SERVICE_SWITCH_ON.matcher(messageText)
        val m2 = Instruction.SERVICE_SWITCH_OFF.matcher(messageText)
        val m3 = Instruction.SERVICE_SWITCH_LIST.matcher(messageText)

        data.value = if (m.find()) {
            getParam(event, m, ON)
        } else if (m2.find()) {
            getParam(event, m2, OFF)
        } else if (m3.find()) {
            getParam(event, m3, LIST)
        } else null

        return data.value != null
    }

    override fun handleMessage(
        event: MessageEvent,
        param: SwitchParam
    ): ServiceCallStatistic? {
        event.reply(param.handle())

        return null
    }

    companion object {
        private val log = LoggerFactory.getLogger(ServiceCallStatistic::class.java)
    }
}
