package com.now.nowbot.throwable.serviceException

import com.now.nowbot.throwable.TipsException

open class BQQueryException(message: String) : TipsException(message) {
    var expression: String? = null

    override val message: String = message
        get() {
            if (expression == null) return field
            return "Expression parsing exception: $expression $field"
        }

    class ParsingBlockException() : BQQueryException("parsing invalid: unsupported operator or value")
    class ParsingQuoteException(end: String) : BQQueryException("parsing invalid: unclosed quote '$end'")

    class UnsupportedKey(key: String) : BQQueryException("unsupported key '$key'")
    class UnsupportedOperator(key: String, value: String) : BQQueryException("'$key' unsupported operator: $value")
    class UnsupportedRankValue(value: String) : BQQueryException("'rank' unsupported value: $value (ss->x / ssh->xh)")
    class UnsupportedScoreValue(value: String) : BQQueryException("'score' unsupported value: $value")
    class UnsupportedIndexValue(value: String) : BQQueryException("'index' unsupported value: $value")
    class UnsupportedModOperator(value: String) : BQQueryException("'mod' unsupported operator: $value")
}