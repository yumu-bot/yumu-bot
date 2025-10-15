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
import com.now.nowbot.throwable.TipsException
import com.now.nowbot.util.DataUtil.getMarkdownFile
import com.now.nowbot.util.DataUtil.getPicture
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.OfficialInstruction
import com.now.nowbot.util.command.REG_EXCLAMATION
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
        val isSimplified: Boolean = false,
    )

    override fun isHandle(event: MessageEvent, messageText: String, data: DataValue<HelpParam>): Boolean {
        val m = Instruction.HELP.matcher(messageText)
        val m2 = Instruction.REFRESH_HELP.matcher(messageText)

        if (m2.find() && Permission.isSuperAdmin(event.sender.id)) {
            imageCacheProvider.clearCache()
            event.reply("已清除所有帮助文档的图片缓存。")
            return false
        }

        if (!m.find()) return false

        val module = m.group("module")
            ?.trim { it <= ' ' }
            ?.lowercase() ?: ""

        val isSimplified = module.isEmpty() && messageText.contains("$REG_EXCLAMATION\\s*(helps?|帮助)\\s*".toRegex())

        data.value = HelpParam(module, isSimplified)
        return true
    }

    @Throws(Throwable::class)
    override fun handleMessage(event: MessageEvent, param: HelpParam): ServiceCallStatistic? {
        if (param.isSimplified) try {
            val image = imageService.getPanelA6(getMarkdownFile("Help/SIMPLIFIED_GUIDE.md"), "help")

            event.reply(image)

            return ServiceCallStatistic.building(event)
        } catch (_: Exception) {

        }

        try {
            val image = getHelpPicture(param.module, imageService) ?: throw TipsException("窝趣，找不到文件")

            event.reply(image)
        } catch (_: TipsException) {
            val imgLegacy = getHelpImageLegacy(param.module)
            val msgLegacy = getHelpLinkLegacy(param.module)

            if (imgLegacy != null) {
                event.reply(imgLegacy)
            }

            if (msgLegacy != null) {
                event.reply(msgLegacy).recallIn((110 * 1000).toLong())
            }
        } catch (_: NullPointerException) {
            val imgLegacy = getHelpImageLegacy(param.module)
            val msgLegacy = getHelpLinkLegacy(param.module)

            if (imgLegacy != null) {
                event.reply(imgLegacy)
            }

            if (msgLegacy != null) {
                event.reply(msgLegacy).recallIn((110 * 1000).toLong())
            }
        } catch (e: Exception) {
            log.error("Help A6 输出错误，使用默认方法也出错？", e)
        }

        return ServiceCallStatistic.building(event)
    }

    /**
     * 目前的 help 方法，走 panel A6
     * @param module 需要查询的功能名字
     * @return 图片流
     */
    private fun getHelpPicture(module: String, imageService: ImageService): ByteArray? {
        val fileName = when (module) {
            "interbot", "inter", "it", "因特" -> "interbot"
            "maomaobot", "meowbot", "meow", "maomao", "kanonbot", "kanon", "cat", "kn", "猫猫", "猫猫bot" -> "kanonbot"
            "superdalou", "dalou", "daloubot", "superdaloubot", "dl", "大楼" -> "superdaloubot"
            "hydrantbot", "hydrant", "hydro", "hy", "xfs", "xf", "~", "消防栓" -> "hydrantbot"
            "cabbage", "白菜", "baicai", "妈船", "妈船？", "mothership", "mother ship", "bc" -> "cabbagebot"
            "bot", "b", "内部指令", "内部" -> "bot"
            "score", "s", "成绩指令", "成绩" -> "score"
            "player", "p", "玩家指令", "玩家" -> "player"
            "map", "m", "谱面指令", "谱面" -> "map"
            "chat", "c", "聊天指令", "聊天" -> "chat"
            "fun", "f", "娱乐指令", "娱乐" -> "fun"
            "aid", "a", "辅助指令", "辅助" -> "aid"
            "tournament", "t", "比赛指令", "比赛" -> "tournament"
            "ping", "pi" -> "ping"
            "bind", "bi" -> "bind"
            "ban", "bq", "bu", "bg" -> "ban"
            "switch", "sw" -> "switch"
            "antispam", "as" -> "antispam"
            "mode", "setmode", "sm", "mo" -> "mode"
            "pass", "pr", "ps" -> "pass"
            "recent", "re", "r" -> "recent"
            "scores", "ss" -> "scores"
            "bestperformance", "bp" -> "bestperformance"
            "todaybp", "tbp", "tb" -> "todaybp"
            "bpanalysis", "bpa", "ba" -> "bpanalysis"
            "information", "info", "i" -> "info"
            "immapper", "imapper", "im" -> "immapper"
            "friend", "friends", "fr" -> "friend"
            "mutual", "mu" -> "mutual"
            "ppminus", "ppm", "pm" -> "ppminus"
            "ppplus", "ppp" -> "ppplus"
            "maps" -> "maps"
            "audio", "song", "au" -> "audio"
            "search", "sh" -> "search"
            "course", "co" -> "course"
            "danacc", "da" -> "danacc"
            "qualified", "q" -> "qualified"
            "leader", "l" -> "leader"
            "match", "ma" -> "match"
            "rating", "mra", "ra" -> "rating"
            "series", "sra", "sa" -> "series"
            "matchlisten", "listen", "ml", "li" -> "listen"
            "matchnow", "now", "mn" -> "matchnow"
            "matchround", "round", "ro", "mr" -> "round"
            "mappool", "pool", "po" -> "pool"
            "oldavatar", "oa" -> "oldavatar"
            "overrating", "oversr", "or" -> "overrating"
            "trans", "tr" -> "trans"
            "kita", "k" -> "kita"

            "OFFICIAL" -> "OFFICIAL"
            else -> "GUIDE"
        }

        val image = imageCacheProvider.getImage(fileName) {
            imageService.getPanelA6(getMarkdownFile("Help/${fileName}.md"), "help")
        }

        return image
    }

    /**
     * 老旧的 help 方法，可以备不时之需
     * @param module 需要查询的功能名字
     * @return 图片流
     */
    private fun getHelpImageLegacy(module: String?): ByteArray? {
        val fileName = when (module) {
            "bot", "b" -> "help-bot"
            "score", "s" -> "help-score"
            "player", "p" -> "help-player"
            "map", "m" -> "help-map"
            "chat", "c" -> "help-chat"
            "fun", "f" -> "help-fun"
            "aid", "a" -> "help-aid"
            "tournament", "t" -> "help-tournament"
            "" -> "help-default"
            else -> ""
        }

        val image = imageCacheProvider.getImage(fileName) {
            getPicture("${fileName}.png")
        }

        return image
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(HelpService::class.java)

        /**
         * 老旧的 help 方法，可以备不时之需
         * @param module 需要查询的功能名字
         * @return 请参阅：link
         */
        private fun getHelpLinkLegacy(module: String?): String? {
            val web = "https://docs.365246692.xyz/help/"
            val link = when (module) {
                "bot", "b" -> "bot"
                "score", "s" -> "score"
                "player", "p" -> "player"
                "map", "m" -> "map"
                "chat", "c" -> "chat"
                "fun", "f" -> "fun"
                "aid", "a" -> "aid"
                "tournament", "t" -> "tournament"
                else -> ""
            }

            //这个是细化的功能
            val link2 = when (module) {
                "help", "h" -> "bot.html#help"
                "ping", "pi" -> "bot.html#ping"
                "bind", "bi" -> "bot.html#bind"
                "ban", "bq", "bu", "bg" -> "bot.html#ban"
                "switch", "sw" -> "bot.html#switch"
                "antispam", "as" -> "bot.html#antispam"
                "mode", "setmode", "sm", "mo" -> "score.html#mode"
                "pass", "pr" -> "score.html#pass"
                "recent", "re" -> "score.html#recent"
                "score", "s" -> ""
                "scorehelp" -> "score.html#score"
                "bestperformance", "bp" -> "score.html#bestperformance"
                "todaybp", "tbp" -> "score.html#todaybp"
                "bpanalysis", "bpa", "ba" -> "score.html#bpanalysis"
                "information", "info", "i" -> "player.html#info"
                "immapper", "imapper", "im" -> "player.html#immapper"
                "friend", "friends", "fr" -> "player.html#friend"
                "mutual", "mu" -> "player.html#mutual"
                "ppminus", "ppm", "pm" -> "player.html#ppminus"
                "ppplus", "ppp" -> "player.html#ppplus"
                "map", "m" -> ""
                "maphelp" -> "map.html#map"
                "audio", "song", "au" -> "map.html#audio"
                "search", "sh" -> "map.html#search"
                "course", "c" -> "map.html#course"
                "danacc", "da" -> "map.html#danacc"
                "qualified", "q" -> "map.html#qualified"
                "leader", "l" -> "map.html#leader"
                "match", "ma" -> "tournament.html#match"
                "rating", "mra", "ra" -> "tournament.html#rating"
                "series", "sra", "sa" -> "tournament.html#series"
                "matchlisten", "listen", "ml", "li" -> "tournament.html#listen"
                "matchnow", "now", "mn" -> "tournament.html#matchnow"
                "matchround", "round", "ro", "mr" -> "tournament.html#round"
                "mappool", "pool", "po" -> "tournament.html#pool"
                "oldavatar", "oa" -> "aid.html#oldavatar"
                "overrating", "oversr", "or" -> "aid.html#overrating"
                "trans", "tr" -> "aid.html#trans"
                "kita", "k" -> "aid.html#kita"
                else -> ""
            }

            return if (link.isEmpty() && link2.isEmpty()) {
                "请参阅：${web}"
            } else if (!link.isEmpty() && link2.isEmpty()) {
                "请参阅：${web}${link}.html"
            } else if (link.isEmpty()) {
                "请参阅功能介绍：${web}${link2}"
            } else {
                null
            }
        }
    }

    override fun accept(event: MessageEvent, messageText: String): HelpParam? {
        return if (OfficialInstruction.HELP.matcher(messageText).find()) {
            HelpParam("OFFICIAL", false)
        } else {
            null
        }
    }

    override fun reply(event: MessageEvent, param: HelpParam): MessageChain? {
        val pic = getHelpPicture(param.module, imageService)

        return if (pic == null) {
            MessageChain("未找到对应的帮助文档。推荐您前往官方网站查询。")
        } else {
            MessageChain(pic)
        }

    }
}
