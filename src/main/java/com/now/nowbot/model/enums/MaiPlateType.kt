package com.now.nowbot.model.enums

import com.now.nowbot.model.maimai.MaiScore
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException

enum class MaiPlateType(val character: String, val required: String) {
    GOKU("極", "fc"), // 极=fc
    SHIN("神", "ap"), // 神=ap
    MAIMAI("舞舞", "fsd"), // 舞=fdx
    SHOU("将", "sss"), // 将=sss
    HASHA("覇", "pass") // 覇
    ;

    companion object {
        fun getPlateType(input: String = ""): MaiPlateType {
            return when(input.lowercase().trim()) {
                "fc", "極", "极" -> GOKU
                "ap", "神" -> SHIN
                "fsd", "fdx", "dx", "舞舞" -> MAIMAI
                "pass", "覇", "覇者", "霸", "霸者" -> HASHA
                "" -> throw NoSuchElementException.MaiPlateType()
                else -> SHOU
            }
        }

        fun isCompleted(plate: MaiPlateType, score: MaiScore?): Boolean {
            if (score == null) return false

            return when(plate) {
                GOKU -> score.combo.isNotEmpty()
                SHIN -> score.combo == "ap" || score.combo == "app"
                MAIMAI -> score.sync == "fsd" || score.sync == "fsdp"
                SHOU -> score.achievements >= 100.0
                HASHA -> score.achievements >= 80.0
            }
        }

        fun getPlateName(version: MaiVersion, plate: MaiPlateType): String {
            return if (version == MaiVersion.ALL_FINALE && plate == HASHA) {
                "覇者"
            } else {
                version.abbreviation + plate.character
            }
        }
    }
}