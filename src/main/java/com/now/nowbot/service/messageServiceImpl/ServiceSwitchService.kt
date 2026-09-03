package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.dao.BindDao
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.restrict.RestrictImplement
import com.now.nowbot.restrict.RestrictRecordEntity
import com.now.nowbot.restrict.RestrictSourceType
import com.now.nowbot.restrict.RestrictTargetType
import com.now.nowbot.restrict.RestrictTargetType.*
import com.now.nowbot.restrict.RestrictUtils.isGroupAdmin
import com.now.nowbot.restrict.RestrictUtils.isSuperAdmin
import com.now.nowbot.restrict.RestrictionController
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.ServiceSwitchService.Operate.*
import com.now.nowbot.service.messageServiceImpl.ServiceSwitchService.ServiceType.Companion.serviceList
import com.now.nowbot.service.messageServiceImpl.ServiceSwitchService.SwitchParam
import com.now.nowbot.throwable.TipsException
import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.throwable.botRuntimeException.PermissionException
import com.now.nowbot.util.AsyncMessageUtil
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.FLAG_DATA
import com.now.nowbot.util.command.FLAG_NAME
import com.now.nowbot.util.command.FLAG_QQ_GROUP
import com.now.nowbot.util.command.FLAG_QQ_ID
import com.now.nowbot.util.command.FLAG_SERVICE
import com.now.nowbot.util.command.REGEX_SPACE_MORE
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.regex.Matcher
import kotlin.time.Clock

@Service("SWITCH")
class ServiceSwitchService(
    val controller: RestrictionController,
    val bindDao: BindDao,
    val imageService: ImageService,
    val restrictImplement: RestrictImplement,
) : MessageService<SwitchParam> {

    enum class ServiceType(private val typeAlias: List<String>, val services: List<String>) {
        BOT(listOf("o", "bot", "内部", "机器"),
            listOf("help", "ping", "bind", "switch", "revoke", "echo", "service_count", "sys_info", "login", "check", "update", "fetch", "refresh_file")
        ),

        SCORE(listOf("s", "scores", "成绩", "分数"),
            listOf("set_mode", "sb_set_mode", "set_group_mode", "score_pr", "sb_score_pr", "pr_card", "uu_pr", "score", "sb_score", "uu_score", "bp", "sb_bp", "today_bp", "sb_today_bp", "bp_analysis", "uu_ba", "bp_analysis_legacy", "bp_fix", "top_plays", "recent_best", "bp_history")
        ),

        PLAYER(listOf("p", "player", "players", "user", "users", "玩家", "用户"),
            listOf("info", "info_card", "uu_info", "i_mapper", "friend", "mutual", "skill", "pp_minus", "team", "badge", "guest_difficulty", "get_id", "get_name", "pp_plus")
        ),

        BEATMAP(listOf("b", "beatmap", "maps", "beatmaps", "谱面", "图"),
            listOf("map", "audio", "explore", "qualified_map", "nomination", "map_minus", "leader_board", "group_leader_board", "get_cover", "view", "refresh_file")
        ),

        MATCH(listOf("m", "match", "tour", "tournament", "matches", "multiplayer", "mp", "比赛", "房间"),
            listOf("mu_rating", "series_rating", "match_listener", "match_now", "match_recent", "match_round", "get_pool")
        ),

        CHAT(listOf("c", "chat", "chats", "聊天"),
            listOf()
        ),

        FUN(listOf("f", "fun", "amuse", "amusement", "娱乐", "玩"),
            listOf("dice", "guess")
        ),

        MAIMAI(listOf("i", "mai", "maimai", "chu", "chunithm", "舞萌", "舞萌中二", "中二", "中二节奏"),
            listOf("mai_audio", "mai_bp", "mai_score", "mai_dist", "mai_find", "mai_version", "mai_versus", "mai_filter", "chu_bp", "mai_seek")
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
            val typeMap = entries.associateWith { it.typeAlias }

            fun getTypeFromInput(input: String): ServiceType? {
                val standard = input.lowercase()

                typeMap.forEach { (type, typeAlias) ->
                    if (typeAlias.contains(standard)) {
                        return type
                    }
                }

                return null
            }

            val serviceList = entries
                .flatMap { it.services }
                .mapTo(sortedSetOf()) { it.uppercase() }
                .toList()
        }
    }

    enum class Operate {
        ON, OFF, LIST
    }

    data class SwitchParam(
        val services: List<String>,
        val operate: Operate,
        val target: RestrictTargetType,
        val source: RestrictSourceType,
        val targetID: Long,
        val sourceID: Long,
        val reason: String = "",
    )

    private fun getSourceType(event: MessageEvent): RestrictSourceType {
        return if (event.isSuperAdmin()) {
            RestrictSourceType.ADMIN
        } else if (event.isGroupAdmin()) {
            RestrictSourceType.GROUP
        } else RestrictSourceType.USER
    }

    private fun getParam(event: MessageEvent, matcher: Matcher, operate: Operate): SwitchParam {
        val sourceType = getSourceType(event)

        val reason = matcher.group(FLAG_DATA) ?: ""

        if (operate == LIST && sourceType != RestrictSourceType.ADMIN) {
            throw PermissionException.DeniedException.BelowSuperAdministrator()
        }

        val (targetType, targetID) = getTarget(event, matcher, sourceType)

        val targetStr = if (targetType == ALL) {
            "所有人"
        } else {
            when (targetType) {
                USER -> "qq 用户："
                GROUP -> "qq 群组："
            } + targetID
        }

        val services = getServices(event, targetStr, matcher.group(FLAG_SERVICE), operate)
            .get(60, TimeUnit.SECONDS)

        return SwitchParam(services.filterNot { it == "GLOBAL" || it.isBlank() }, operate, targetType, sourceType, targetID, event.sender.contactID, reason)
    }

    private fun getTarget(event: MessageEvent, matcher: Matcher, sourceType: RestrictSourceType): Pair<RestrictTargetType, Long> {
        val qq: Long? = if (event.hasAt()) {
            event.target
        } else {
            matcher.group(FLAG_QQ_ID)?.toLongOrNull()
        }

        val group: Long? = matcher.group(FLAG_QQ_GROUP)?.toLongOrNull()

        val name: String = matcher.group(FLAG_NAME) ?: ""

        if (sourceType == RestrictSourceType.ADMIN) {
            if (qq != null) {
                return USER to qq
            } else if (group != null) {
                return GROUP to group
            } else if (name.isNotBlank()) {
                val user = bindDao.getBindUserOrNull(name.trim()) ?: throw TipsException("""
                对方没有绑定。请使用 qq= 来确定对方的 QQ。
            """.trimIndent())

                return USER to bindDao.getQQ(user)
            } else {
                return ALL to 0L
            }
        } else {
            if (qq != null && qq != event.sender.contactID) {
                throw TipsException("""
                    权限不足！只有超级管理员拥有控制其他 QQ 用户的权限。
                """.trimIndent())
            } else if (group != null && group != event.subject.contactID) {
                throw TipsException("""
                    权限不足！只有超级管理员拥有控制其他 QQ 群聊的权限。
                """.trimIndent())
            } else if (name.isNotBlank()) {
                throw TipsException("""
                    权限不足！只有超级管理员拥有控制其他 osu! 用户的权限。
                """.trimIndent())
            } else {
                // 选择操作模式

                if (sourceType == RestrictSourceType.USER) {
                    // 普通用户只能控制自己
                    return USER to event.sender.contactID
                } else {
                    if (qq == event.sender.contactID) {
                        return USER to qq
                    } else if (group == event.subject.contactID) {
                        return GROUP to group
                    }

                    // 这里必定是群聊管理员
                    // 群聊管理员可以控制群聊的开关，所以在模棱两可的时候，需要询问。

                    val future = CompletableFuture<Pair<RestrictTargetType, Long>>()

                    AsyncMessageUtil.doubleCheck(
                        event = event,
                        onCheck = {
                            event.reply("""
                                您需要操作群聊还是操作您自己呢？
                                回复 1 操作群聊，回复 2 操作自己。
                            """.trimIndent())
                        },

                        onWrong = {
                            future.completeExceptionally(TipsException("操作已中止。"))
                        },

                        onOverTime = {
                            future.completeExceptionally(TipsException("确认超时。"))
                        },

                        onSuccess = { ev ->
                            if (ev.rawMessage.contains("1", ignoreCase = true)) {
                                future.complete(GROUP to event.subject.contactID)
                            } else if (ev.rawMessage.contains("2", ignoreCase = true)) {
                                future.complete(USER to event.subject.contactID)
                            } else {
                                future.completeExceptionally(TipsException("操作已中止。"))
                            }
                        }
                    )

                    return future.get()
                }
            }
        }
    }

    private fun getServices(event: MessageEvent, target: String, input: String?, operate: Operate): CompletableFuture<List<String>> {
        if (operate == LIST) {
            return CompletableFuture.completedFuture(emptyList())
        }

        val future = CompletableFuture<List<String>>()

        val ii = (input ?: "GLOBAL").trim().replace(REGEX_SPACE_MORE, "_").uppercase()

        val type = ServiceType.getTypeFromInput(ii)

        val does = when(operate) {
            OFF -> "关闭"
            ON -> "开启"
            // else -> "查看"
        }

        if (ii.isEmpty() || ii == "GLOBAL") {
            // 全局操作模式

            AsyncMessageUtil.doubleCheck(
                event = event,
                onCheck = {
                    event.reply("""
                        您确定要${does}所有服务吗？回复 OK 确认。
                        操作对象：${target}
                        操作服务：所有
                        
                        如果不想${does}所有服务，也可以尝试按类别来${does}。
                        """.trimIndent())
                },

                onWrong = {
                    future.completeExceptionally( TipsException("${does}操作已中止。"))
                },

                onOverTime = {
                    future.completeExceptionally(TipsException("确认超时。"))
                },

                onSuccess = {
                    future.complete(emptyList())
                }
            )

            return CompletableFuture.completedFuture(emptyList())
        }

        if (type == null) {
            val service = serviceList.firstOrNull { it == ii }

            if (service != null) {
                // 单服务操作模式
                return CompletableFuture.completedFuture(listOf(ii))
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
            AsyncMessageUtil.doubleCheck(
                event = event,
                onCheck = { event.reply("""
                    您确定要${does} ${type.name} 类别下的所有服务吗？回复 OK 确认。
                    操作对象：${target}
                    操作服务：${type.services.joinToString(", ")}
                    """.trimIndent())
                },

                onWrong = {
                    future.completeExceptionally(TipsException("${does}操作已中止。"))
                },

                onOverTime = {
                    future.completeExceptionally(TipsException("确认超时。"))
                },

                onSuccess = {
                    future.complete(type.services.map { it.uppercase() })
                }
            )
        }

        return future
    }

    private fun SwitchParam.handleOn(): MessageChain {
        if (this.services.isEmpty()) {
            controller.unblock(this.target, this.targetID, "GLOBAL", this.source)
        } else {
            this.services.forEach { service ->
                controller.unblock(this.target, this.targetID, service, this.source)
            }
        }

        val targetStr = when(this.target) {
            ALL -> ""
            USER -> "用户 ${this.targetID} 的 "
            GROUP -> "群聊 ${this.targetID} 的 "
        }

        val serviceStr = if (this.services.isEmpty()) {
            "所有服务"
        } else {
            "${services.joinToString(", ")} 服务"
        }

        return MessageChain("操作已完成：开启${targetStr}${serviceStr}")
    }

    private fun SwitchParam.handleOff(): MessageChain {
        if (this.services.isEmpty()) {
            controller.block(this.target, this.targetID, "GLOBAL",
                this.source, this.sourceID, null, Clock.System.now(), null
            )
        } else {
            this.services.forEach { service ->
                controller.block(this.target, this.targetID, service,
                    this.source, this.sourceID, null, Clock.System.now(), null
                )
            }
        }

        val targetStr = when(this.target) {
            ALL -> ""
            USER -> "用户 ${this.targetID} 的"
            GROUP -> "群聊 ${this.targetID} 的"
        }

        val serviceStr = if (this.services.isEmpty()) {
            "所有服务"
        } else {
            " ${services.joinToString(", ")} 服务"
        }

        return MessageChain("操作已完成：关闭${targetStr}${serviceStr}")
    }

    private fun handleList(): MessageChain {
        val restricted = controller.findAllRestricting()

        val global = RestrictView.fromEntities(restricted.filter { it.service == "GLOBAL" })
        val filtered = RestrictView.fromEntities(restricted.filter { it.service != "GLOBAL" })

        val markdown = getStatisticsList(filtered, global)

        val image = try {
            imageService.getPanelA6(markdown, "switch")
        } catch (e: Exception) {
            log.error("服务控制：渲染失败", e)
            throw IllegalStateException.Render("服务控制")
        }

        return MessageChain(image)
    }

    private fun SwitchParam.handle(): MessageChain {

        return try {
            when(this.operate) {
                ON -> this.handleOn()
                OFF -> this.handleOff()
                LIST -> handleList()
            }
        } finally {
            restrictImplement.refreshCache()
        }
    }

    private fun getStatisticsList(list: List<RestrictView>, global: List<RestrictView>): String {
        val sb = StringBuilder()

        sb.append("## 服务列表\n\n")
        sb.append("| 状态 | 服务名 | 无法使用的群聊 | 无法使用的用户 |\n")
        sb.append("| :-: | :-- | :-- | :-- |\n")

        // 格式化辅助函数：截取前 5 项，无限制则显示 "-"
        fun formatIds(ids: Collection<Long>): String =
            if (ids.isEmpty()) "-" else ids.take(5).joinToString(", ")

        // 1. 优先渲染 GLOBAL 顶级行（如果存在）
        if (global.isNotEmpty()) {
            val mergedGroups = global.flatMap { it.groups }.toSet()
            val mergedUsers = global.flatMap { it.users }.toSet()

            val gs = formatIds(mergedGroups)
            val us = formatIds(mergedUsers)

            sb.append("| * | GLOBAL | $gs | $us |\n")
        }

        // 2. 渲染各服务列表
        list.forEach { view ->
            val status = if (view.enabled) "ON" else "OFF"
            val gs = formatIds(view.groups)
            val us = formatIds(view.users)

            sb.append("| $status | ${view.name} | $gs | $us |\n")
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
        event.replyAsync(param.handle())

        return null
    }

    companion object {
        private val log = LoggerFactory.getLogger(ServiceCallStatistic::class.java)
    }
}

data class RestrictView(
    val name: String,
    val enabled: Boolean = true,
    val groups: Set<Long> = emptySet(),
    val users: Set<Long> = emptySet()
) {
    companion object {
        /**
         * 将实体列表（可能包含重名 service 的多条记录）按 service 聚合为 View 列表
         */
        fun fromEntities(
            entities: List<RestrictRecordEntity>
        ): List<RestrictView> {
            val now = Clock.System.now()

            // 1. 按 service 分组聚合有效记录
            val aggregated = entities
                .filter { it.isCurrentlyActive(now) }
                .groupBy { it.service }
                .mapValues { (serviceName, records) ->
                    val groups = mutableSetOf<Long>()
                    val users = mutableSetOf<Long>()

                    for (record in records) {
                        when (record.targetType) {
                            USER.byte -> users.add(record.targetID)
                            GROUP.byte -> groups.add(record.targetID)
                        }
                    }
                    RestrictView(
                        name = serviceName,
                        enabled = true,
                        groups = groups,
                        users = users
                    )
                }

            // 2. 依照 serviceList 顺序构建结果
            return serviceList.map { serviceName ->
                aggregated[serviceName] ?: RestrictView(name = serviceName)
            } + aggregated.filterKeys { it !in serviceList.toSet() }.values
        }
    }
}
