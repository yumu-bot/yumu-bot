package com.now.nowbot.model.enums

enum class GreekChar(val capital: String, val small: String, val romanised: String, val full: String) {
    ALPHA("Α", "α", "a", "alpha"),
    BETA("Β", "β", "b", "beta"),
    GAMMA("Γ", "γ", "g", "gamma"),
    DELTA("Δ", "δ", "d", "delta"),
    EPSILON("Ε", "ε", "e", "epsilon"),
    ZETA("Ζ", "ζ", "z", "zeta"),
    ETA("Η", "η", "h", "eta"),
    THETA("Θ", "θ", "th", "theta"),
    IOTA("Ι", "ι", "i", "iota"),
    KAPPA("Κ", "κ", "k", "kappa"),
    LAMBDA("Λ", "λ", "l", "lambda"),
    MU("Μ", "μ", "m", "mu"),
    NU("Ν", "ν", "n", "nu"),
    XI("Ξ", "ξ", "x", "xi"),
    OMICRON("Ο", "ο", "o", "omicron"),
    PI("Π", "π", "p", "pi"),
    RHO("Ρ", "ρ", "r", "rho"),
    SIGMA("Σ", "σ", "s", "sigma"),
    SIGMA2("Σ", "ς", "s", "sigma"),
    TAU("Τ", "τ", "t", "tau"),
    UPSILON("Υ", "υ", "u", "upsilon"),
    PHI("Φ", "φ", "f", "phi"),
    CHI("Χ", "χ", "x", "chi"),
    PSI("Ψ", "ψ", "ps", "psi"),
    OMEGA("Ω", "ω", "o", "omega"),
    ;

    companion object {
        private val GREEK_LOOKUP_TABLE = Array<String?>(65536) { null }.apply {
            for (g in entries) {
                val targetValue = g.romanised

                // 1. 注册大写字符映射
                if (g.capital.isNotEmpty()) {
                    this[g.capital[0].code] = targetValue
                }
                // 2. 注册小写字符映射
                if (g.small.isNotEmpty()) {
                    this[g.small[0].code] = targetValue
                }
            }
        }

        fun getRomanized(greek: String?): String {
            if (greek.isNullOrEmpty()) return ""

            val len = greek.length
            val sb = StringBuilder(len * 2)

            for (i in 0 until len) {
                val ch = greek[i]
                val code = ch.code

                // O(1) 直接查表拿转换后的字符串
                val mapped = if (code < 65536) GREEK_LOOKUP_TABLE[code] else null

                if (mapped != null) {
                    sb.append(mapped)
                } else {
                    sb.append(ch)
                }
            }

            return sb.toString()
        }

        fun CharSequence.appendRomanizedGreekTo(target: StringBuilder) {
            val len = this.length
            for (i in 0 until len) {
                val ch = this[i]
                val code = ch.code

                // 直接查静态数组（GREEK_LOOKUP_TABLE 是 Array<String?>）
                val mapped = if (code < 65536) GreekChar.GREEK_LOOKUP_TABLE[code] else null

                if (mapped != null) {
                    target.append(mapped)
                } else {
                    target.append(ch)
                }
            }
        }

        fun isEqual(char: Char, greekChar: GreekChar): Boolean {
            return (char.toString() == greekChar.capital || char.toString() == greekChar.small)
        }

        fun containsGreek(chars: CharSequence?): Boolean {
            if (chars.isNullOrEmpty()) return false

            val len = chars.length
            for (i in 0 until len) {
                val code = chars[i].code
                if (code in 0x0370..0x03FF || code in 0x1F00..0x1FFF) {
                    return true
                }
            }
            return false
        }
    }
}
