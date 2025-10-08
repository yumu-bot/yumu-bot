package com.now.nowbot.service.messageServiceImpl

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.mikuac.shiro.core.BotContainer
import com.mikuac.shiro.dto.action.response.GroupMemberInfoResp
import com.now.nowbot.config.FileConfig
import com.now.nowbot.config.NewbieConfig
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.osu.MicroUser
import com.now.nowbot.qq.contact.Group
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.TipsException
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.JacksonUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.SynchronousSink
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

@Service("GROUP_STATISTICS")
class GroupStatisticsService(
    private val osuApiWebClient: WebClient,
    private val bots: BotContainer,
    private val userApiService: OsuUserApiService,
    private val newbieConfig: NewbieConfig,
    fileConfig: FileConfig,
) : MessageService<Long> {
    private val cachePath: Path = Path.of(fileConfig.root, "StatisticalOverPPService.json")

    private fun getOsuId(qq: Long): Long? {
        if (UserCache.containsKey(qq)) {
            return UserCache[qq]
        }
        val id = osuApiWebClient.get()
            .uri(GET_BINDING, qq)
            .retrieve()
            .bodyToMono(JsonNode::class.java)
            .handle { json: JsonNode, sink: SynchronousSink<Long?> ->
                if (!json.hasNonNull("userId")) {
                    sink.error(WebClientResponseException.create(404, "NOT FOUND", HttpHeaders(), byteArrayOf(), null))
                    return@handle
                }
                sink.next(json["osuId"].asLong())
            }
            .block()
        UserCache[qq] = id
        return id
    }

    fun getOsuBp1(osuId: Long): Float {
        return osuApiWebClient.get()
            .uri(GET_BP_URL, osuId)
            .retrieve()
            .bodyToMono(JsonNode::class.java)
            .handle { json: JsonNode, sink: SynchronousSink<Double> ->
                if (!json.isArray || json.isEmpty) {
                    sink.error(WebClientResponseException.create(404, "NOT FOUND", HttpHeaders(), byteArrayOf(), null))
                    return@handle
                }
                val b1 = json[0]
                sink.next(b1["pp"].asDouble(0.0))
            }
            .block()!!.toFloat()
    }

    @Throws(Throwable::class) override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: DataValue<Long>
    ): Boolean {
        if (event.subject !is Group || lock != 0) {
            return false
        }
        val m = Instruction.GROUP_STATISTICS.matcher(messageText)
        if (m.find()) {
            when (m.group("group")) {
                "a", "进阶群" -> data.value = newbieConfig.advancedGroup
                "h", "高阶群" -> data.value = newbieConfig.hyperGroup
                "n", "新人群" -> data.value = newbieConfig.newbieGroup
                null -> {
                    return false
                }
            }
            lock = 3
            return true
        }
        lock = 0
        return false
    }

    @Throws(Throwable::class) override fun handleMessage(event: MessageEvent, param: Long): ServiceCallStatistic? {
        // init 搬到这里来，别每次启动都要存个文件到那里
        try {
            if (Files.isRegularFile(cachePath)) {
                val jsonStr = Files.readString(cachePath)

                val cache = JacksonUtil.parseObject(
                    jsonStr,
                    object : TypeReference<HashMap<Long, Long>>() {
                    })
                if (cache != null) {
                    UserCache.putAll(cache)
                }
            } else {
                Files.createFile(cachePath)
            }
        } catch (e: IOException) {
            log.error("文件操作失败", e)
        }

        val from = event.subject
        if (from is Group) {
            try {
                work(from, param)
            } catch (e: Exception) {
                log.error("出现错误:", e)
            } finally {
                lock = 0
                Files.writeString(cachePath, JacksonUtil.toJson(UserCache)!!)
            }
        }

        return ServiceCallStatistic.building(event)
    }

    @Throws(Exception::class) private fun work(group: Group, groupId: Long) {
        val bot = bots.robots[newbieConfig.hydrantBot]
        if (bot == null) {
            group.sendMessage("主bot未在线")
            return
        } else {
            val targetGroup = bot.getGroupInfo(groupId, true).data
            if (Objects.isNull(targetGroup) || targetGroup.memberCount <= 0) {
                throw TipsException("获取群信息失败, 可能未加入此群")
            }
        }
        group.sendMessage("开始统计: $groupId")

        var groupInfo: List<GroupMemberInfoResp>? = null
        for (i in 0..4) {
            try {
                groupInfo = bot.getGroupMemberList(groupId).data
            } catch (e: Exception) {
                continue
            }
            if (groupInfo == null) break
        }
        if (groupInfo == null) throw TipsException("获取群成员失败")
        groupInfo = groupInfo.filter { r: GroupMemberInfoResp -> r.role.equals("member", ignoreCase = true) }
        val checkPoints = groupInfo.size / 5
        // qq-info
        val users: MutableMap<Long, MicroUser?> = HashMap(groupInfo.size)
        // qq-bp1
        val usersBP1: MutableMap<Long, Float> = HashMap(groupInfo.size)
        // uid-qq
        val nowOsuId: MutableMap<Long, Long> = HashMap(150)
        // qq-err
        val errMap: MutableMap<Long?, String?> = HashMap()

        var count = 0
        for (u in groupInfo) {
            val qq = u.userId
            val id: Long
            try {
                Thread.sleep(1000)
                id = getOsuId(qq)!!
                val bp1 = getOsuBp1(id)
                nowOsuId[id] = qq
                usersBP1[qq] = bp1
                log.debug("统计 {} 信息获取成功. bp1 {}pp", qq, bp1)
            } catch (err: WebClientResponseException.NotFound) {
                //这个err不需要记录下来 修改了日志等级, 默认不记录
                log.debug("统计 {} 未找到: {}", qq, err.message)
                if (err.message.contains("bleatingsheep.org")) {
                    errMap[qq] = "未绑定"
                } else {
                    errMap[qq] = "osu信息查询不到, 可能已删号"
                }
                users[qq] = null
            } catch (e: Exception) {
                log.error("统计出现异常: {}", qq, e)
                errMap[qq] = e.message
                users[qq] = null
            }
            count++
            if (count % checkPoints == 0) {
                group.sendMessage(String.format("%d 统计进行到 %.2f%%", groupId, 100f * count / groupInfo.size))
            }
            if (nowOsuId.size >= 50) {
                val result = userApiService.getUsers(nowOsuId.keys)
                for (uInfo in result) {
                    users[nowOsuId[uInfo.userID] ?: continue] = uInfo
                }
                nowOsuId.clear()
            }
        }

        val sb = StringBuilder("qq,id,data,pp,bp1\n")

        users.entries
            .sortedByDescending { it.value?.rulesets?.osu?.pp ?: 0.0 }
            .forEach { entry: Map.Entry<Long?, MicroUser?> ->
                sb.append('\'').append(entry.key).append(',')
                if (entry.value == null) {
                    val s = errMap[entry.key]
                    sb.append("加载失败").append(s).append('\n')
                    return@forEach
                }
                sb.append(entry.value!!.userID).append(',')
                sb.append(entry.value!!.username).append(',')
                sb.append(entry.value!!.rulesets!!.osu!!.pp).append(',')
                sb.append(usersBP1[entry.key]).append('\n')
            }
        group.sendFile(sb.toString().toByteArray(StandardCharsets.UTF_8), "$groupId.csv")
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(GroupStatisticsService::class.java)
        private const val GET_BINDING = "https://api.bleatingsheep.org/api/Binding/{qq}"
        private const val GET_BP_URL: String = "https://osu.ppy.sh/users/{osuId}/scores/best?mode=osu&limit=1"

        private val UserCache: MutableMap<Long, Long?> = HashMap()

        private var lock = 0
    }
}
