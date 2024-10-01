package com.now.nowbot.model.enums

enum class GreekChar(val capital: String, val small: String, val romanized: String) {
    ALPHA("Α", "α", "a"),
    BETA("Β", "β", "b"),
    GAMMA("Γ", "γ", "g"),
    DELTA("Δ", "δ", "d"),
    EPSILON("Ε", "ε", "e"),
    ZETA("Ζ", "ζ", "z"),
    ETA("Η", "η", "h"),
    THETA("Θ", "θ", "th"),
    IOTA("Ι", "ι", "i"),
    KAPPA("Κ", "κ", "k"),
    LAMBDA("Λ", "λ", "l"),
    MU("Μ", "μ", "m"),
    NU("Ν", "ν", "n"),
    XI("Ξ", "ξ", "x"),
    OMICRON("Ο", "ο", "o"),
    PI("Π", "π", "p"),
    RHO("Ρ", "ρ", "r"),
    SIGMA("Σ", "σ", "s"),
    SIGMA2("Σ", "ς", "s"),
    TAU("Τ", "τ", "t"),
    UPSILON("Υ", "υ", "u"),
    PHI("Φ", "φ", "f"),
    CHI("Χ", "χ", "x"),
    PSI("Ψ", "ψ", "ps"),
    OMEGA("Ω", "ω", "o"),
    ;

    companion object {
        @JvmStatic
        fun getRomanized(greek: String?): String {
            if (greek.isNullOrEmpty()) {
                return ""
            }

            if (! containsGreek(greek)) {
                return greek
            }

            val sb = StringBuilder()

            for(s in greek.toCharArray()) {
                for(g in GreekChar.entries) {
                    if (isEqual(s, g)) {
                        sb.append(g.romanized)
                        continue
                    }
                }
            }

            return sb.toString()
        }

        private fun isEqual(char: Char, greekChar: GreekChar): Boolean {
            return (char.toString() == greekChar.capital || char.toString() == greekChar.small)
        }

        private fun containsGreek(T: CharSequence): Boolean {
            return T.contains(Regex("[\u0370-\u03ff]"))
        }
    }
}
