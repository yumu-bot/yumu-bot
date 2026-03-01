package com.now.nowbot.controller

import com.fasterxml.jackson.databind.JsonNode
import com.now.nowbot.dao.BindDao
import com.now.nowbot.model.BindResponse
import com.now.nowbot.model.BindUser
import com.now.nowbot.service.messageServiceImpl.BindService.BindData
import com.now.nowbot.service.messageServiceImpl.BindService.Companion.getBind
import com.now.nowbot.service.messageServiceImpl.BindService.Companion.removeBind
import com.now.nowbot.service.osuApiService.OsuUserApiService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.lang.Nullable
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.reactive.function.client.WebClientResponseException

@ResponseBody
@RestController
@RequestMapping(produces = ["application/json;charset=UTF-8"])
@ConditionalOnProperty(value = ["yumu.osu.callbackPath"])
class BindController @Autowired constructor(var userApiService: OsuUserApiService, var bindDao: BindDao) {
    @GetMapping("bindUrl")
    fun bindUrl(): String {
        return userApiService.getOauthUrl("yumu")
    }

    @PostMapping("bindBack")
    fun newBind(@RequestParam("code") refreshToken: String?): BindResponse {
        var user = BindUser(refreshToken)

        var result: BindResponse

        try {
            userApiService.refreshUserTokenFirst(user)

            val oldUser = bindDao.getBindUser(user.userID)

            if (oldUser == null) {
                user = bindDao.saveBind(user)
            } else {
                oldUser.username = user.username
                oldUser.accessToken = user.accessToken
                oldUser.refreshToken = user.refreshToken
                oldUser.time = user.time
                user = bindDao.saveBind(oldUser)
            }

            result = BindResponse(
                user.baseID!!,
                user.userID,
                user.username,
                user.mode.shortName,
                "绑定成功!"
            )
        } catch (e: Exception) {
            log.error("绑定时异常", e)
            result = BindResponse(
                -1,
                -1,
                "",
                "",
                "绑定失败, 请重试, 如果一直失败请联系开发者."
            )
        }
        return result
    }

    @GetMapping("bindCode")
    fun bindCode(@RequestParam("id") id: Long?, @RequestParam("di") di: Long): String {
        val user: BindUser?
        try {
            user = bindDao.getBindUserByDbId(id)
            if (user == null || di != user.userID) {
                return "你不许绑定"
            }
        } catch (e: Exception) {
            log.error("绑定查找出错: ", e)
            return "人机不要来绑定!"
        }
        return bindDao.generateCaptcha(user.userID)
    }

    @GetMapping("\${yumu.osu.callbackPath}")
    fun bind(@RequestParam("code") code: String?, @RequestParam("state") stat: String): String? {
        val data: Array<String?> = stat.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (data.size != 2) {
            return "噶?"
        }
        return saveBind(code, data[1]!!)
    }

    fun saveBind(code: String?, keyStr: String): String {
        val sb = StringBuilder()

        val key: Long = keyStr.toLongOrNull() ?: return "非法访问：参数异常"

        val msg = getBind(key)

        if (DEBUG) {
            return doBind(sb, code, msg!!, key)
        }

        if (msg == null) {
            return """
                绑定链接失效。
                请重新绑定。
                当然也有可能你已经绑好了。出去可以试试功能。
                """.trimIndent()
        } else {
            try {
                msg.receipt.recall()
            } catch (e: Exception) {
                log.error("绑定消息撤回失败", e)
                return """
                    绑定连接已超时。
                    请重新绑定。
                    """.trimIndent()
            }
        }

        return doBind(sb, code, msg, key)
    }

    private fun doBind(sb: StringBuilder, code: String?, msg: BindData, key: Long): String {
        try {
            val bd = BindUser(code)

            userApiService.refreshUserTokenFirst(bd)

            val qqBind = bindDao.bindQQ(msg.qq, bd)
            removeBind(key)
            sb.append("成功绑定:\n<br/>")
                .append(msg.qq)
                .append(" -> ")
                .append(bd.username)
                .append("\n<br/>")
                .append("您的默认游戏模式为：[")
                .append(qqBind.osuUser!!.mode.shortName).append("]。")
                .append("\n<br/>")
                .append("如果您不是主模式 [osu] 玩家，请使用 `!ymmode [mode]` 来修改默认模式。否则可能会影响您查询成绩。")
                .append("\n<br/>")
                .append("[mode]：0 osu(standard)，1 taiko，2 catch，3 mania")
        } catch (e: HttpClientErrorException.BadRequest) {
            log.error("绑定时异常：400", e)
            sb.append("出现异常。但您大概已经绑定成功。这可能是回执的问题。")
                .append('\n')
                .append(e.localizedMessage)
        } catch (e: WebClientResponseException.BadRequest) {
            log.error("绑定时异常：400", e)
            sb.append("出现异常。但您大概已经绑定成功。这可能是回执的问题。")
                .append('\n')
                .append(e.localizedMessage)
        } catch (e: Exception) {
            log.error("绑定时异常：未知", e)
            sb.append("出现未知异常，请截图给开发者让他抓紧修 BUG。错误代码和信息：")
                .append('\n')
                .append(e.localizedMessage)
        }
        return sb.toString()
    }

    @PostMapping("/api")
    fun opa(
        @RequestHeader("state") @Nullable stat: String?,
        @RequestBody @Nullable body: JsonNode?
    ): String? {
        var data: Array<String?>? = null
        var code: String? = null

        try {
            if (stat != null) data = stat.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (body != null) code = body.get("code").asText()
        } catch (e: Exception) {
            return e.message
        }


        if (data == null || data.size != 2) {
            return "蛤"
        }

        val ret = saveBind(code, data[1]!!)
        log.info("绑定api端口被访问,参数: state->{} code->{}:{}", stat, code, ret)
        return ret
    }

    @PostMapping("/gitup")
    fun update(@RequestBody body: JsonNode) {
        log.info("收到一条推送\n{}", body.toString())
    }

    companion object {
        const val DEBUG: Boolean = false
        val log: Logger = LoggerFactory.getLogger(BindController::class.java)
    }
}
