package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.config.Permission
import com.now.nowbot.service.ImageCacheProvider
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.util.DataUtil.getMarkdownFile
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.OfficialInstruction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service("HELP")
class HelpService(
    private val imageService: ImageService,
    private val imageCacheProvider: ImageCacheProvider,
) : MessageService<HelpService.HelpParam>, TencentMessageService<HelpService.HelpParam> {

    data class HelpParam(
        val module: String,
    )

    // 以后这里会放子模组的帮助页面，长大了再写
    private fun getModule(module: String?): String? {
        return when (module?.trim()?.lowercase()) {
            "interbot", "inter", "it", "因特" -> "interbot"
            "maomaobot", "meowbot", "meow", "maomao", "kanonbot", "kanon", "cat", "kn", "猫猫", "猫猫bot" -> "kanonbot"
            // "superdalou", "dalou", "daloubot", "superdaloubot", "dl", "大楼" -> "superdaloubot"
            // "hydrantbot", "hydrant", "hydro", "hy", "xfs", "xf", "~", "消防栓" -> "hydrantbot"
            // "cabbage", "白菜", "baicai", "妈船", "妈船？", "mothership", "mother ship", "bc" -> "cabbagebot"

            "" -> ""

            // 未收录的返回空，有的机器人他的帮助是 !帮助 2 这样的，会串台
            else -> null
        }
    }

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: DataValue<HelpParam>
    ): Boolean {
        val m = Instruction.HELP.matcher(messageText)
        val m2 = Instruction.SIMPLIFIED_HELP.matcher(messageText)
        val mr = Instruction.REFRESH_HELP.matcher(messageText)

        if (mr.find() && Permission.isSuperAdmin(event.sender.id)) {
            imageCacheProvider.clearCache()
            event.reply("已清除所有帮助文档的图片缓存。")
            return false
        }

        if (m.find()) {
            val module = getModule(m.group("module")) ?: return false

            data.value = HelpParam(module.ifBlank { "GUIDE" })
            return true
        } else if (m2.find()) {
            val module = getModule(m.group("module")) ?: return false

            data.value = HelpParam(module.ifBlank { "SIMPLIFIED_GUIDE" })
            return true
        }

        return false
    }

    override fun handleMessage(
        event: MessageEvent,
        param: HelpParam
    ): ServiceCallStatistic? {
        val image = try {
            imageCacheProvider.getImage(param.module) {
                imageService.getPanelA6(getMarkdownFile("Help/${param.module}.md"), "help")
            }
        } catch (e: Exception) {
            log.error("帮助文档：获取失败", e)
            throw IllegalStateException.Fetch("帮助文档")
        }

        try {
            event.reply(image)
        } catch (e: Exception) {
            log.error("帮助文档：发送失败", e)
            throw IllegalStateException.Send("帮助文档")
        }

        return ServiceCallStatistic.building(event)
    }

    override fun accept(event: MessageEvent, messageText: String): HelpParam? {
        return if (OfficialInstruction.HELP.matcher(messageText).find()) {
            HelpParam("OFFICIAL")
        } else {
            null
        }
    }

    override fun reply(event: MessageEvent, param: HelpParam): MessageChain? {
        val image = try {
            imageCacheProvider.getImage(param.module) {
                imageService.getPanelA6(getMarkdownFile("Help/${param.module}.md"), "help")
            }
        } catch (e: Exception) {
            log.error("帮助文档：获取失败", e)
            null
        } ?: return MessageChain("未找到对应的帮助文档。推荐您前往官方网站查询。")

        return MessageChain(image)
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(HelpService::class.java)
    }
}
