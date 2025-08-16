package com.now.nowbot.model.enums

enum class MaiCabinet {
    DX,
    SD,
    ANY,

    ;

    companion object {
        fun getCabinet(string: String?): MaiCabinet {
            if (string.isNullOrBlank()) return ANY

            return when(string.lowercase()) {
                "sd", "标准", "standard", "标" -> SD
                "dx", "豪华", "deluxe" -> DX
                else -> ANY
            }
        }
    }
}