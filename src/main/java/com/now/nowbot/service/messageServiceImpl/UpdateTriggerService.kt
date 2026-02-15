package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.config.Permission
import com.now.nowbot.dao.OsuUserInfoDao
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.DailyStatisticsService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.divingFishApiService.ChunithmApiService
import com.now.nowbot.service.divingFishApiService.MaimaiApiService
import com.now.nowbot.service.lxnsApiService.LxMaiApiService
import com.now.nowbot.service.messageServiceImpl.UpdateTriggerService.UpdateType.*
import com.now.nowbot.throwable.botRuntimeException.PermissionException
import com.now.nowbot.throwable.botRuntimeException.UnsupportedOperationException
import com.now.nowbot.util.DataUtil
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.FLAG_ANY
import org.springframework.stereotype.Service

@Service("UPDATE")
class UpdateTriggerService(
    private val maimaiApiService: MaimaiApiService,
    private val lxMaiApiService: LxMaiApiService,
    private val chunithmApiService: ChunithmApiService,
    private val dailyStatisticsService: DailyStatisticsService,
    private val infoDao: OsuUserInfoDao,
) : MessageService<UpdateTriggerService.UpdateType> {

    enum class UpdateType {
        MAIMAI, LXNS, OSU_PERCENT, OSU_DAILY;

        companion object {
            fun getType(string: String?): UpdateType {
                return when(string?.trim()) {
                    "m", "mai", "maimai" -> MAIMAI
                    "l", "lx", "lxns", "luoxue", "lady" -> LXNS
                    "o", "p", "percent", "per" -> OSU_PERCENT
                    "d", "daily" -> OSU_DAILY
                    else -> throw UnsupportedOperationException("""
                        请输入需要更新的种类：
                        
                        m -> maimai
                        l -> lxns
                        p -> osu percent
                    """.trimIndent())
                }
            }
        }
    }

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<UpdateType>
    ): Boolean {
        val matcher = Instruction.UPDATE.matcher(messageText)
        if (!matcher.find()) {
            return false
        }

        val any: String? = matcher.group(FLAG_ANY)

        if (Permission.isSuperAdmin(event.sender.contactID)) {
            data.value = UpdateType.getType(any)
            return true
        } else return false
    }

    override fun handleMessage(event: MessageEvent, param: UpdateType): ServiceCallStatistic? {
        when(param) {
            OSU_PERCENT -> Thread.startVirtualThread {
                event.reply("正在尝试更新玩家百分比数据！")
                val count = infoDao.percentilesDailyUpsert()
                event.reply("已更新 $count 条玩家百分比数据。")
            }

            MAIMAI -> Thread.startVirtualThread {
                event.reply(
                    """
                    正在尝试更新舞萌、中二数据！
                    注意，这不会更新落雪歌曲数据库。
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

                event.reply(
                    """
                更新舞萌、中二节奏数据完成。
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

                event.reply(
                    """
                更新落雪数据完成。
                
                歌曲数据库：${DataUtil.time2HMS(time1 - startTime)}
                收藏库：${DataUtil.time2HMS(endTime - time1)}
                总耗时：${DataUtil.time2HMS(endTime - startTime)}
                """.trimIndent()
                )
            }

            OSU_DAILY -> {
                if (!Permission.isSuperAdmin(event.sender.contactID)) {
                    throw PermissionException.DeniedException.BelowSuperAdministrator()
                }

                val startTime = System.currentTimeMillis()

                event.reply("已提交更新指令，系统正在后台处理...")
                dailyStatisticsService.collectInfoAndScores {
                    val endTime = System.currentTimeMillis()
                    event.reply("每日数据更新已完成，耗时：${DataUtil.time2HMS(endTime - startTime)}")
                }

            }
        }

        return null
    }
}
