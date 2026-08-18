package com.now.nowbot.model.enums

enum class LeaderBoardType(val value: String) {
    GLOBAL("global"),
    FRIEND("friend"),
    COUNTRY("country"),
    TEAM("team"),

    ;

    companion object {
        fun getType(string: String?): LeaderBoardType {
            return when(string?.trim()?.lowercase()) {
                "country", "countries", "c" -> COUNTRY
                "friend", "friends", "f" -> FRIEND
                "team", "clan", "t" -> TEAM
                else -> GLOBAL
            }
        }
    }
}
