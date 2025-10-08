package com.now.nowbot.service.messageServiceImpl

import com.fasterxml.jackson.databind.JsonNode
import com.now.nowbot.aop.CheckPermission
import com.now.nowbot.dao.BindDao
import com.now.nowbot.entity.DrawLogLite
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.mapper.DrawLogLiteRepository
import com.now.nowbot.model.DrawConfig
import com.now.nowbot.model.enums.DrawGrade
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.JacksonUtil
import jakarta.annotation.Resource
import org.springframework.stereotype.Service
import java.util.*
import java.util.function.Consumer
import java.util.regex.Matcher

@Service("DRAW")
class DrawService : MessageService<Matcher> {
    @Resource
    private val bindDao: BindDao? = null

    @Resource
    private val drawLogLiteRepository: DrawLogLiteRepository? = null

    override fun isHandle(event: MessageEvent, messageText: String, data: DataValue<Matcher>): Boolean {
        val m = Instruction.DRAW.matcher(messageText)
        if (m.find()) {
            data.value = m
            return true
        } else return false
    }

    @CheckPermission(test = true) @Throws(Throwable::class) override fun handleMessage(
        event: MessageEvent,
        param: Matcher
    ): ServiceCallStatistic? {
        val bindUser = bindDao!!.getBindFromQQ(event.sender.id, true)

        var times = 1
        if (param.group("d") != null) {
            times = param.group("d").toInt()
        }

        val tenTimes = times / 10
        times %= 10
        val clist: MutableList<DrawConfig.Card> = LinkedList()
        // 10 连
        for (i in 0..<tenTimes) {
            val gradeList = defaultConfig!!.getGrade10(bindUser.userID, drawLogLiteRepository)
            val cards = gradeList.stream().map { grade: DrawGrade? -> defaultConfig.getCard(grade) }.toList()
            val cardLites = ArrayList<DrawLogLite>(gradeList.size)
            for (j in gradeList.indices) {
                cardLites.add(DrawLogLite(cards[i], gradeList[i], bindUser.userID))
            }
            drawLogLiteRepository!!.saveAll(cardLites)
            clist.addAll(cards)
        }
        // 单抽
        run {
            for (i in 0..<times) {
                val grade = defaultConfig!!.getGrade(bindUser.userID, drawLogLiteRepository)
                val card = defaultConfig.getCard(grade)
                drawLogLiteRepository!!.save(DrawLogLite(card, grade, bindUser.userID))
                clist.add(card)
            }
        }

        val sb = StringBuilder()
        clist.forEach(Consumer { c: DrawConfig.Card -> sb.append(c.name).append(", ") })
        event.reply(sb.toString())
        return ServiceCallStatistic.building(event)
    }

    companion object {
        private fun getConfig(config: String): DrawConfig? {
            val jsonData = JacksonUtil.parseObject(
                config,
                JsonNode::class.java
            )
            if (jsonData == null) return null

            return DrawConfig(jsonData)
        }

        var config: String = """
            {
              "N": {
                "name": "普通",
                "weight": 15,
                "cards": [
                  {
                    "name": "n1",
                    "weight": 100,
                    "info": "n"
                  },
                  {
                    "name": "n2",
                    "weight": 100,
                    "info": "n"
                  },
                  {
                    "name": "n3",
                    "weight": 100,
                    "info": "n"
                  },
                  {
                    "name": "n4",
                    "weight": 100,
                    "info": "n"
                  },
                  {
                    "name": "n5",
                    "weight": 100,
                    "info": "n"
                  },
                  {
                    "name": "n6",
                    "weight": 100,
                    "info": "n"
                  },
                  {
                    "name": "n7",
                    "weight": 100,
                    "info": "n"
                  },
                  {
                    "name": "n8",
                    "weight": 100,
                    "info": "n"
                  }
                ]
              },
              "R": {
                "name": "稀有",
                "weight": 4,
                "cards":[
                  {
                    "name": "r1",
                    "weight": 100,
                    "info": "r"
                  },
                  {
                    "name": "r2",
                    "weight": 100,
                    "info": "r"
                  },
                  {
                    "name": "r3",
                    "weight": 100,
                    "info": "r"
                  }
                ]
              },
              "SR": {
                "name": "传说",
                "weight": 1,
                "cards":[
                  {
                    "name": "sr1",
                    "weight": 100,
                    "info": "n"
                  }
                ]
              },
              "SSR": {
                "name": "传说",
                "weight": 1,
                "cards":[
                  {
                    "name": "ssr1",
                    "weight": 100,
                    "info": "n"
                  }
                ]
              }
            }
            
            """.trimIndent()
        val defaultConfig: DrawConfig? = getConfig(config)
    }
}
