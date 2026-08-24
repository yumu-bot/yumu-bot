package com.now.nowbot.model.enums

import com.now.nowbot.model.osu.LazerMod
import com.now.nowbot.model.osu.LazerMod.Companion.containsHidden
import com.now.nowbot.model.osu.LazerScore

enum class OsuGrade {
    SSH, SS, SH, S, A, B, C, D, F;

    fun toString(mods: List<LazerMod> = emptyList()): String {
        if (mods.containsHidden()) {
            if (this == SS) return SSH.name
            if (this == S) return SH.name
        }

        return this.name
    }

    companion object {
        fun getApproximateRank(score: LazerScore): OsuGrade {
            val t = score.statistics
            val m = score.mode.safeModeValue
            val h = score.mods.containsHidden()
            val l = score.isLazer

            return if (l) {
                when(m) {
                    0.toByte() -> {
                        val total = t.great + t.ok + t.meh + t.miss
                        val noMiss = t.miss == 0

                        val accuracy = score.accuracy * 100.0

                        when {
                            total == t.great -> if (h) SSH else SS
                            accuracy >= 95.0 -> if (noMiss) {
                                if (h) SH else S
                            } else {
                                A
                            }
                            accuracy >= 90.0 -> A
                            accuracy >= 80.0 -> B
                            accuracy >= 70.0 -> C
                            else -> D
                        }
                    }

                    1.toByte() -> {
                        val total = t.great + t.ok + t.miss
                        val noMiss = t.miss == 0

                        val accuracy = score.accuracy * 100.0

                        when {
                            total == t.great -> if (h) SSH else SS
                            accuracy >= 95.0 -> if (noMiss) {
                                if (h) SH else S
                            } else {
                                A
                            }
                            accuracy >= 90.0 -> A
                            accuracy >= 80.0 -> B
                            accuracy >= 70.0 -> C
                            else -> D
                        }
                    }

                    2.toByte() -> {
                        val miss = t.largeTickMiss + t.smallTickMiss + t.miss

                        val accuracy = score.accuracy * 100.0

                        when {
                            miss == 0 -> if (h) SSH else SS
                            accuracy >= 98.0 -> if (h) SH else S
                            accuracy >= 94.0 -> A
                            accuracy >= 90.0 -> B
                            accuracy >= 85.0 -> C
                            else -> D
                        }
                    }

                    3.toByte() -> {
                        val total = t.perfect + t.great + t.good + t.ok + t.meh + t.miss

                        val accuracy = score.accuracy * 100.0

                        when {
                            total == (t.perfect + t.great) -> if (h) SSH else SS
                            accuracy >= 95.0 -> if (h) SH else S
                            accuracy >= 90.0 -> A
                            accuracy >= 80.0 -> B
                            accuracy >= 70.0 -> C
                            else -> D
                        }
                    }

                    else -> F
                }
            } else {
                when(m) {
                    0.toByte() -> {
                        val total = t.great + t.ok + t.meh + t.miss

                        if (total == 0) {
                            return D
                        }

                        val p300 = t.great * 100.0 / total
                        val p50 = t.meh * 100.0 / total
                        val noMiss = t.miss == 0

                        when {
                            t.great == total -> if (h) SSH else SS

                            p300 >= 90.0 && p50 <= 1 && noMiss -> if (h) SH else S

                            (p300 >= 80.0 && noMiss) || (p300 > 90.0) -> A

                            (p300 >= 70.0 && noMiss) || (p300 > 80.0) -> B

                            p300 >= 60.0 -> C

                            else -> D
                        }
                    }

                    1.toByte() -> {
                        val total = t.great + t.ok + t.miss

                        if (total == 0) {
                            return D
                        }

                        val p300 = t.great * 100.0 / total
                        val noMiss = t.miss == 0

                        when {
                            t.great == total -> if (h) SSH else SS

                            p300 >= 90.0 && noMiss -> if (h) SH else S

                            (p300 >= 80.0 && noMiss) || (p300 >= 90.0) -> A

                            (p300 >= 70.0 && noMiss) || (p300 >= 80.0) -> B

                            p300 >= 60.0 -> C

                            else -> D
                        }
                    }

                    // 必须是大于，而不能等于
                    2.toByte() -> {
                        val miss = t.largeTickMiss + t.smallTickMiss + t.miss

                        val accuracy = score.accuracy * 100.0

                        when {
                            miss == 0 -> if (h) SSH else SS
                            accuracy > 98.0 -> if (h) SH else S
                            accuracy > 94.0 -> A
                            accuracy > 90.0 -> B
                            accuracy > 85.0 -> C
                            else -> D
                        }
                    }

                    3.toByte() -> {
                        val total = t.perfect + t.great + t.good + t.ok + t.meh + t.miss

                        val accuracy = score.accuracy * 100.0

                        when {
                            total == t.perfect -> if (h) SSH else SS
                            accuracy >= 95.0 -> if (h) SH else S
                            accuracy >= 90.0 -> A
                            accuracy >= 80.0 -> B
                            accuracy >= 70.0 -> C
                            else -> D
                        }
                    }

                    else -> F
                }
            }
        }
    }
}

