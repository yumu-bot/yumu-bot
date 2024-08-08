package com.now.nowbot.newbie.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import org.hibernate.proxy.HibernateProxy
import java.time.LocalDateTime

@Entity(name = "UserPlayRecords")
data class UserPlayRecords(
    @Id
    @Column(name = "Id")
    var id: Long?,

    @Column(name = "UserId")
    var userId: Int?,
    @Column(name = "PlayNumber")
    var playNumber: Int?,
    @Column(name = "Mode")
    var mode: Int?,
    @Column(name = "Record_BeatmapId")
    var beatmapId: Int?,
    @Column(name = "Record_Score")
    var score: Long?,
    @Column(name = "Record_MaxCombo")
    var maxCombo: Int?,
    @Column(name = "Record_Count50")
    var count50: Int?,
    @Column(name = "Record_Count100")
    var count100: Int?,
    @Column(name = "Record_Count300")
    var count300: Int?,
    @Column(name = "Record_CountMiss")
    var countMiss: Int?,
    @Column(name = "Record_CountKatu")
    var countKatu: Int?,
    @Column(name = "Record_CountGeki")
    var countGeki: Int?,
    @Column(name = "Record_Perfect")
    var perfect: Boolean?,
    @Column(name = "Record_EnabledMods")
    var mods: Int?,
    @Column(name = "Record_UserId")
    var recordUserId: Int?,
    @Column(name = "Record_Date")
    var date: LocalDateTime?,
    @Column(name = "Record_Rank")
    var rank: String?
) {
    final override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        val oEffectiveClass =
            if (other is HibernateProxy) other.hibernateLazyInitializer.persistentClass else other.javaClass
        val thisEffectiveClass =
            if (this is HibernateProxy) this.hibernateLazyInitializer.persistentClass else this.javaClass
        if (thisEffectiveClass != oEffectiveClass) return false
        other as UserPlayRecords

        return id != null && id == other.id
    }

    final override fun hashCode(): Int =
        if (this is HibernateProxy) this.hibernateLazyInitializer.persistentClass.hashCode() else javaClass.hashCode()
}

@Entity(name = "Bindings")
data class Bindings(
    @Id
    @Column(name = "UserId")
    var userId: Long?,
    @Column(name = "OsuId")
    var osuId: Int?,

)