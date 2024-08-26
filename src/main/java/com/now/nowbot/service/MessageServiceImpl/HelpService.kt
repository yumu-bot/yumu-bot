package com.now.nowbot.service.MessageServiceImpl
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.throwable.TipsException
import com.now.nowbot.util.DataUtil.getMarkdownFile
import com.now.nowbot.util.DataUtil.getPicture
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.OfficialInstruction
import com.now.nowbot.util.QQMsgUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.lang.Nullable
import org.springframework.stereotype.Service
import java.util.*

@Service("HELP") 
class HelpService(
    var imageService:ImageService? = null
) : MessageService<String>, TencentMessageService<String> {

    override fun isHandle(event:MessageEvent, messageText:String, data:DataValue<String>): Boolean {
        val m = Instruction.HELP.matcher(messageText)
        if (m.find()) {
            data.setValue(m.group("module").trim{it <= ' '}.lowercase(Locale.getDefault())) //传东西进来
            return true
        } else return false
    }
    
    @Throws(Throwable::class)
    override fun HandleMessage(event:MessageEvent, module : String) {
         val from = event.subject

         try {
             val image = getHelpPicture(module, imageService)
            
            if (Objects.nonNull(image)) {
                from.sendImage(image)
            } else {
                throw TipsException("窝趣，找不到文件")
            }
        } catch (e : TipsException) {
             val imgLegacy = getHelpImageLegacy(module)
             val msgLegacy = getHelpLinkLegacy(module)
            
            if (Objects.nonNull(imgLegacy)) {
                from.sendImage(imgLegacy)
            }
            
            if (Objects.nonNull(msgLegacy)) {
                from.sendMessage(msgLegacy).recallIn((110 * 1000).toLong())
            }
        } catch (e : NullPointerException) {
             val imgLegacy = getHelpImageLegacy(module)
             val msgLegacy = getHelpLinkLegacy(module)
            
            if (Objects.nonNull(imgLegacy)) {
                from.sendImage(imgLegacy)
            }
            
            if (Objects.nonNull(msgLegacy)) {
                from.sendMessage(msgLegacy).recallIn((110 * 1000).toLong())
            }
        } catch (e : Exception) {
            log.error("Help A6 输出错误，使用默认方法也出错？", e)
        }
    }
    
    companion object {
        private val log:Logger = LoggerFactory.getLogger(HelpService::class.java)
        
        /**
         * 目前的 help 方法，走 panel A6
         * @param module 需要查询的功能名字
         * @return 图片流
         */
        private fun getHelpPicture(module:String, imageService:ImageService?): ByteArray? {
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
            
            return try {
                imageService!!.getPanelA6(getMarkdownFile("Help/${fileName}.md"), "help")
            } catch (e : Exception) {
                null
            }
        }
        
        /**
        * 老旧的 help 方法，可以备不时之需
        * @param module 需要查询的功能名字
        * @return 图片流
        */
        private fun getHelpImageLegacy(@Nullable module:String): ByteArray? {
            val fileName = when (module) {"bot", "b" -> "help-bot"
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
            
            return getPicture("${fileName}.png")
        }
        
        /**
        * 老旧的 help 方法，可以备不时之需
        * @param module 需要查询的功能名字
        * @return 请参阅：link
        */
        private fun getHelpLinkLegacy(@Nullable module:String): String? {
            val web = "https://docs.365246692.xyz/help/"
            val link = when (module){
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

    override fun accept(event: MessageEvent, messageText: String) : String? {
        if (OfficialInstruction.HELP.matcher(messageText).find()) return "OFFICIAL";
        else return null;
    }

    override fun reply(event: MessageEvent, data: String): MessageChain? {
        return QQMsgUtil.getImage(getHelpPicture(data, imageService))
    }
}
