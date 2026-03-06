package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.config.Permission
import com.now.nowbot.dao.OsuUserInfoDao
import com.now.nowbot.dao.ScoreDao
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.DailyStatisticsService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.divingFishApiService.ChunithmApiService
import com.now.nowbot.service.divingFishApiService.MaimaiApiService
import com.now.nowbot.service.lxnsApiService.LxMaiApiService
import com.now.nowbot.service.messageServiceImpl.UpdateTriggerService.UpdateType.*
import com.now.nowbot.throwable.botRuntimeException.PermissionException.DeniedException.*
import com.now.nowbot.throwable.botRuntimeException.UnsupportedOperationException
import com.now.nowbot.util.ASyncMessageUtil
import com.now.nowbot.util.DataUtil
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.FLAG_ANY
import com.now.nowbot.util.command.FLAG_MODE
import org.springframework.stereotype.Service

@Service("UPDATE")
class UpdateTriggerService(
    private val maimaiApiService: MaimaiApiService,
    private val lxMaiApiService: LxMaiApiService,
    private val chunithmApiService: ChunithmApiService,
    private val dailyStatisticsService: DailyStatisticsService,
    private val infoDao: OsuUserInfoDao,
    private val scoreDao: ScoreDao,
) : MessageService<Pair<UpdateTriggerService.UpdateType, String?>> {

    enum class UpdateType {
        DIVING_FISH, LXNS, OSU_PERCENT, OSU_DAILY, OSU_STAR_RATING;

        companion object {
            fun getType(string: String?): UpdateType {
                return when(string?.trim()) {
                    "m", "mai", "maimai" -> DIVING_FISH
                    "l", "lx", "lxns", "luoxue", "lady" -> LXNS
                    "o", "p", "percent", "per" -> OSU_PERCENT
                    "d", "daily" -> OSU_DAILY
                    "s", "r", "star", "sr", "rating" -> OSU_STAR_RATING
                    else -> throw UnsupportedOperationException("""
                        请输入需要更新的种类：
                        
                        m -> maimai
                        l -> lxns
                        p -> osu percent
                        d -> osu daily
                        r -> flush osu star rating
                        t -> repair osu statistics missing
                    """.trimIndent())
                }
            }
        }
    }

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<Pair<UpdateType, String?>>
    ): Boolean {
        val matcher = Instruction.UPDATE.matcher(messageText)
        if (!matcher.find()) {
            return false
        }

        if (Permission.isSuperAdmin(event.sender.contactID)) {
            val any: String? = matcher.group(FLAG_MODE)

            data.value = UpdateType.getType(any) to matcher.group(FLAG_ANY)
            return true
        } else return false
    }

    override fun handleMessage(event: MessageEvent, param: Pair<UpdateType, String?>): ServiceCallStatistic? {
        when(param.first) {
            OSU_PERCENT -> {
                event.reply("正在尝试更新玩家百分比数据！")
                val count = infoDao.percentilesDailyUpsert()
                event.reply("已更新 $count 条玩家百分比数据。")
            }

            DIVING_FISH -> {
                event.reply(
                    """
                    正在尝试更新水鱼数据！
                    注意，这不会更新落雪数据。
                    """.trimIndent()
                )

                val startTime = System.currentTimeMillis()

                maimaiApiService.updateMaimaiSongLibraryDatabase()
                val time1 = System.currentTimeMillis()
                maimaiApiService.updateMaimaiAliasLibraryDatabase()
                val time2 = System.currentTimeMillis()
                maimaiApiService.updateMaimaiRankLibraryDatabase()
                val time3 = System.currentTimeMillis()
                maimaiApiService.updateMaimaiFitLibraryDatabase()
                val time4 = System.currentTimeMillis()
                chunithmApiService.updateChunithmSongLibraryDatabase()
                val time5 = System.currentTimeMillis()
                chunithmApiService.updateChunithmAliasLibraryDatabase()

                val endTime = System.currentTimeMillis()

                event.reply("""
                    更新水鱼数据完成。
                    舞萌歌曲库：${DataUtil.time2HMS(time1 - startTime)}
                    舞萌外号库：${DataUtil.time2HMS(time2 - time1)}
                    舞萌玩家排名库：${DataUtil.time2HMS(time3 - time2)}
                    舞萌拟合定数库：${DataUtil.time2HMS(time4 - time3)}
                    中二节奏歌曲数据库：${DataUtil.time2HMS(time5 - time4)}
                    中二节奏外号库：${DataUtil.time2HMS(endTime - time5)}
                    
                    总耗时：${DataUtil.time2HMS(endTime - startTime)}
                    """.trimIndent()
                )
            }

            LXNS -> {
                event.reply("正在尝试更新落雪数据！")

                val startTime = System.currentTimeMillis()
                lxMaiApiService.saveLxMaiSongs()
                val time1 = System.currentTimeMillis()
                lxMaiApiService.saveLxMaiCollections()
                val endTime = System.currentTimeMillis()

                event.reply("""
                    更新落雪数据完成。
                    
                    歌曲数据库：${DataUtil.time2HMS(time1 - startTime)}
                    收藏库：${DataUtil.time2HMS(endTime - time1)}
                    总耗时：${DataUtil.time2HMS(endTime - startTime)}
                    """.trimIndent()
                )
            }

            OSU_DAILY -> {
                if (!Permission.isSuperAdmin(event.sender.contactID)) {
                    throw BelowSuperAdministrator()
                }

                ASyncMessageUtil.doubleCheck(
                    event,
                    onCheck = {
                        event.reply("高耗时操作：你确定要开始统计所有玩家的今日数据吗？回复 OK 确认。")
                    },
                    onSuccess = {
                        val startTime = System.currentTimeMillis()

                        event.reply("已提交更新指令，系统正在后台处理...")
                        dailyStatisticsService.collectInfoAndScores {
                            val endTime = System.currentTimeMillis()
                            event.reply("每日数据更新已完成，耗时：${DataUtil.time2HMS(endTime - startTime)}")
                        }
                    }
                )
            }

            OSU_STAR_RATING -> {
                val mode = OsuMode.getMode(param.second)

                val modeStr = if (mode == OsuMode.DEFAULT) {
                    "所有"
                } else {
                    mode.fullName
                }

                ASyncMessageUtil.doubleCheck(
                    event,
                    onCheck = {
                        event.reply("高危操作：你确定要删去 $modeStr 模式的星数吗？回复 OK 确认。")
                    },
                    onSuccess = {
                        val count = scoreDao.deleteByMode(mode)

                        val c = if (count > 0) {
                            "\n总计 $count 条。"
                        } else {
                            ""
                        }

                        event.reply("已经清除 $modeStr 模式的星数。${c}")
                    }
                )
            }
        }

        return null
    }
}
