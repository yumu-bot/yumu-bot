package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.dao.AliasDao
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.throwable.botRuntimeException.IllegalArgumentException
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.util.AsyncMessageUtil
import com.now.nowbot.util.StringUtil.removeSongSuffix
import com.now.nowbot.util.StringUtil.standardised
import org.springframework.stereotype.Service
import kotlin.time.Duration.Companion.seconds

@Service("ALIAS")
class AliasService(
    private val beatmapApiService: OsuBeatmapApiService,

    private val aliasDao: AliasDao,
): MessageService<AliasService.AliasParam> {

    data class AliasParam(
        val title: String?,
        val beatmapID: Long?,
        val alias: String?,
        val operation: AliasOperation
    )

    enum class AliasOperation {
        ADD, DELETE, REVIEW
    }

    private fun check(event: MessageEvent, param: AliasParam) {
        val title = param.title
            ?: beatmapApiService.getBeatmap(param.beatmapID
                ?: throw IllegalArgumentException.WrongException.BeatmapID()
            ).beatmapset!!.title

        if (param.alias.isNullOrEmpty()) {
            throw IllegalArgumentException.WrongException.Alias()
        }

        val standardizedTitle = title.removeSongSuffix().standardised()
        val standardizedAlias = param.alias.removeSongSuffix().standardised()

        AsyncMessageUtil.doubleCheck(
            event = event,
            keyword = "OK",
            onCheck = {
                event.reply("""
                    添加别名：${standardizedTitle} -> $standardizedAlias
                """.trimIndent())
            },
            onSuccess = { ev ->
                aliasDao.addAliasToTitle(standardizedTitle, standardizedAlias, setAsPrimary = false)
                ev.reply("保存成功！")
            },
            timeout = 30.seconds
        )

    }

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<AliasParam>
    ): Boolean {
        TODO("Not yet implemented")
    }

    override fun handleMessage(
        event: MessageEvent,
        param: AliasParam
    ): ServiceCallStatistic? {
        TODO("Not yet implemented")
    }
}