package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.TestTaikoSRCalculateService.Status
import com.now.nowbot.util.Instruction
import org.springframework.stereotype.Service
import java.util.LinkedList
import java.util.regex.Matcher

@Service("TEST_TAIKO_SR_CALCULATE") 
class TestTaikoSRCalculateService : MessageService<Matcher> {
    internal enum class Status {
        XX, OO, XO, OX, X, O, NONE;

        companion object {
            fun get(a: Char, b: Char): Status {
                if (a == CHAR_X) {
                    if (b == CHAR_X) return XX
                    else if (b == CHAR_O) return XO
                    else if (b == CHAR_NONE) return X
                } else if (a == CHAR_O) {
                    if (b == CHAR_X) return OX
                    else if (b == CHAR_O) return OO
                    else if (b == CHAR_NONE) return O
                } else {
                    if (b == CHAR_X) return X
                    else if (b == CHAR_O) return O
                    else if (b == CHAR_NONE) return NONE
                }
                return NONE
            }
        }}
    
    override fun isHandle(event: MessageEvent, messageText: String, data: DataValue<Matcher>): Boolean {
        val m = Instruction.TEST_TAIKO_SR_CALCULATE.matcher(messageText)
        if (m.find()) {
            data.setValue(m)
            return true
        } else return false
    }
    
    @Throws(Throwable::class) override fun HandleMessage(event: MessageEvent, matcher: Matcher) {
        val xtr = matcher.group("data").trim {it <= ' '} .replace("\\s+".toRegex(), CHAR_NONE.toString())
        event.getSubject().sendMessage("结果:" + get(xtr))
    }
    
    private fun get(xtr: String): Float {
        var nexIndex = 4
        val statusList = LinkedList<Status>()
        val datas = CharArray(xtr.length)
        xtr.toCharArray(datas, 0, 0, xtr.length)
                //处理前4个
        if (datas.size <= 3) {
            //?
        } else if (datas[1] == CHAR_NONE) {
            statusList.add(if (datas[0] == CHAR_X) Status.XX else Status.OO)
            statusList.add(Status.NONE)
            statusList.add(Status.Companion.get(datas[2], datas[3]))
            nexIndex = 4
        } else if (datas[2] == CHAR_NONE) {
            statusList.add(Status.Companion.get(datas[0], datas[1]))
            statusList.add(Status.NONE)
            nexIndex = 3
        } else if (datas[3] == CHAR_NONE) {
            statusList.add(Status.Companion.get(datas[0], datas[1]))
            statusList.add(Status.Companion.get(datas[2], datas[3]))
            statusList.add(Status.Companion.get(datas[1], datas[2]))
            statusList.add(Status.NONE)
            nexIndex = 4
        } else {
            statusList.add(Status.Companion.get(datas[0], datas[1]))
            nexIndex = 2
        }
        
                //
        var i = nexIndex
        while (i < datas.size) {
            if ((i + 3) >= datas.size) {
                nexIndex = i
                break
            }
            nexIndex = checkNext(statusList, datas[i], datas[i + 1], datas[i + 2], datas[i + 3])
            i += nexIndex}
        val t = datas.size - nexIndex
        if (t == 1) {
            statusList.add(Status.Companion.get(datas[nexIndex], CHAR_NONE))
        } else if (t == 2) {
            statusList.add(Status.Companion.get(datas[nexIndex], datas[nexIndex + 1]))
        } else if (t == 3) {
            statusList.add(Status.Companion.get(datas[nexIndex], datas[nexIndex + 1]))
            statusList.add(Status.Companion.get(datas[nexIndex + 2], CHAR_NONE))
        }
        
        
                //解析
        var thisStatus = statusList.poll()
        var nextStatus: Status
        
        var a = 0
        var b = 0
        var c = 0
        while ((statusList.poll().also { nextStatus = it }) != null) {
            if (nextStatus == Status.NONE) {
                a++
                b++
                break
            }
            when (thisStatus){Status.OO -> {
                    when (nextStatus){Status.OX -> {
                            b++
                            c++
                            thisStatus = nextStatus}
                        Status.XO -> {
                            a++
                            c++
                            thisStatus = nextStatus}
                        Status.XX -> {
                            b++
                            a++
                            thisStatus = nextStatus}
                        Status.X -> a++
                        else -> {}}
                }
                Status.XX -> {
                    when (nextStatus){Status.OX -> {
                            a++
                            c++
                            thisStatus = nextStatus}
                        Status.XO -> {
                            b++
                            c++
                            thisStatus = nextStatus}
                        Status.OO -> {
                            b++
                            a++
                            thisStatus = nextStatus}
                        Status.O -> a++
                        else -> {}}
                }
                Status.XO -> {
                    when (nextStatus){Status.OX -> {
                            a++
                            b++
                            thisStatus = nextStatus}
                        Status.XX -> {
                            b++
                            c++
                            thisStatus = nextStatus}
                        Status.OO -> {
                            a++
                            c++
                            thisStatus = nextStatus}
                        Status.X -> c++
                        Status.O -> {
                            a++
                            c++
                        }
                        else -> {}}
                }
                Status.OX -> {
                    when (nextStatus){Status.XO -> {
                            a++
                            b++
                            thisStatus = nextStatus}
                        Status.XX -> {
                            a++
                            c++
                            thisStatus = nextStatus}
                        Status.OO -> {
                            b++
                            c++
                            thisStatus = nextStatus}
                        Status.O -> c++
                        Status.X -> {
                            a++
                            c++
                        }
                        else -> {}}
                }

                else -> {}
            }
        }
        return (0.25f * a + b) * (0.5f * c + 1)
    }
    
    private fun checkNext(statusList: MutableList<Status>, a: Char, b: Char, c: Char, d: Char): Int {
        if (c == CHAR_NONE) {
            statusList.add(Status.Companion.get(a, b))
            statusList.add(Status.NONE)
            return 3
        } else if (d == CHAR_NONE) {
            statusList.add(Status.Companion.get(a, b))
            statusList.add(Status.Companion.get(c, d))
            statusList.add(Status.Companion.get(b, c))
            statusList.add(Status.NONE)
            return 4
        } else {
            statusList.add(Status.Companion.get(a, b))
            return 2
        }
    }

    companion object {
        private const val CHAR_X = 'x'
        private const val CHAR_O = 'o'
        private const val CHAR_NONE = '-'
    }}
