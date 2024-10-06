package com.now.nowbot.model.enums

enum class GreekChar(val capital: String, val small: String, val transformation: String, val romanized: String) {
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
        @JvmStatic
        fun getRomanized(greek: String?): String {
            if (greek.isNullOrEmpty()) {
                return ""
            }

            if (! containsGreek(greek)) {
                return greek
            }

            val sb = StringBuilder()

            outro@
            for(s in greek.toCharArray()) {
                for(g in GreekChar.entries) {
                    if (isEqual(s, g)) {
                        sb.append(g.romanized)
                        continue@outro
                    }
                }

                sb.append(s)
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
