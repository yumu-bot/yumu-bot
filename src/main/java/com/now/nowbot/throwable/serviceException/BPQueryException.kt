package com.now.nowbot.throwable.serviceException

import com.now.nowbot.throwable.TipsException

open class BPQueryException(message: String) : TipsException(message) {
    var expression: String? = null

    override val message: String = message
        get() {
            if (expression == null) return field
            return "[$expression] $field"
        }

    class ParsingBlockException() : BPQueryException("分析失败：不支持的运算符或参数")
    class ParsingQuoteException(end: String) : BPQueryException("分析失败：未闭合的引号 '$end'")

    class UnsupportedKey(key: String) : BPQueryException("不支持的参数 '$key'")
    class UnsupportedOperator(key: String, value: String) : BPQueryException("参数 '$key' 不支持使用运算符：$value")
    class UnsupportedOrderKey(key: String) : BPQueryException("参数 '$key' 不支持排序")
    class UnsupportedRankValue(value: String) : BPQueryException("参数 'rank' 不支持使用 $value 匹配 (ss->x / ssh->xh)")
    class UnsupportedScoreValue(value: String) : BPQueryException("参数 'score' 不支持使用 $value 匹配")
    class UnsupportedIndexValue(value: String) : BPQueryException("参数 'index' 不支持使用 $value 匹配")
    class UnsupportedModOperator(value: String) : BPQueryException("参数 'mod' 不支持使用 $value 运算符")
    class NullInput() : BPQueryException("请输入查询参数！")
}