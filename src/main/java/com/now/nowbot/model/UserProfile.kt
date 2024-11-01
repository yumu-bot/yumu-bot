package com.now.nowbot.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.entity.UserProfileItem
import com.now.nowbot.service.messageServiceImpl.CustomService
import com.now.nowbot.util.DataUtil
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KMutableProperty1

data class UserProfile(
    @JsonProperty("card")
    var card: String? = null,

    @JsonProperty("banner")
    var banner: String? = null,

    @JsonProperty("mascot")
    var mascot: String? = null,

    @JsonProperty("header")
    var headerBorder: String? = null,

    @JsonProperty("info")
    var infoPanel: String? = null,

    @JsonProperty("score")
    var scorePanel: String? = null,

    @JsonProperty("ppm")
    var ppmPanel: String? = null,
) {
    enum class Type(val column: String, val field: KMutableProperty1<UserProfile, String?>) {
        CARD("card", UserProfile::card),
        BANNER("banner", UserProfile::banner),
        MASCOT("mascot", UserProfile::mascot),
        HEADER("header", UserProfile::headerBorder),
        INFO("panel_info", UserProfile::infoPanel),
        SCORE("panel_score", UserProfile::scorePanel),
        PPM("panel_ppm", UserProfile::ppmPanel),
    }

    fun setByColumn(column: String, path: String) {
        Type.entries.firstOrNull { it.column == column }?.also {
            it.field.set(this, path)
        }
    }

    fun getColumns(userId: Long): List<UserProfileItem> {
        return Type.entries.mapNotNull {
            it.field.get(this)?.let { path ->
                UserProfileItem(userId, it.column, path)
            }
        }
    }

    companion object {
        val publicPrefix = CustomService.FILE_DIV_PATH.toAbsolutePath().toString().let {
            if (it.endsWith("/")) it else "$it/"
        }
        val publicSuffix = ConcurrentHashMap<String, Char>()

        fun shortPath(id: Long, path: String?): String? {
            if (path == null) return null
            val f = path.substringAfterLast("-")
            val s = publicSuffix.getOrPut(f) { 'a' + publicSuffix.size }
            val i = DataUtil.numberTo62(id)
            return "$s$i"
        }

        fun restorePath(short: String): String {
            val s = short[0]
            val i = DataUtil.stringTo62(short.substring(1))
            return "$publicPrefix$i-$s"
        }

        fun parseVerificationList(columns: List<UserProfileItem>): Map<Long, UserProfile> {
            return columns
                .groupBy { it.userId }
                .map { (userId, items) ->
                    items.forEach { it.path = shortPath(userId, it.path!!) }
                    userId to parse(items)
                }
                .toMap()
        }

        fun parse(columns: List<UserProfileItem>): UserProfile {
            val profile = UserProfile()
            columns
                .filter { it.path != null && it.verify }
                .forEach { profile.setByColumn(it.type, it.path!!) }
            return profile
        }

    }
}
