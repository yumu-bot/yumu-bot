package com.now.nowbot.dao

import com.now.nowbot.entity.NewbieRestrictLite
import com.now.nowbot.mapper.NewbieRestrictRepository
import org.springframework.stereotype.Component
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

@Component
class NewbieDao(private val newbieRestrictRepository: NewbieRestrictRepository) {

    fun saveRestricted(qq: Long?, star: Double?, time: Long?, duration: Long?) {
        val restrict = NewbieRestrict(qq, star, time, duration)
        newbieRestrictRepository.save(restrict.toLite())
    }

    fun getRestricted(qq: Long?): List<NewbieRestrict> {
        return newbieRestrictRepository.findByQQ(qq ?: return emptyList())
            ?.map { NewbieRestrict().fromLite(it) } ?: emptyList()
    }

    /**
     * 获取从现在到之前这段时间内超星了多少次
     * @param time: 毫秒
     */
    fun getRestrictedCountWithin(qq: Long?, time: Duration = Duration.INFINITE): Int {
        val now = System.currentTimeMillis()
        val l = getRestricted(qq)

        return l.count { now - (it.time ?: 0L) <= time.inWholeMilliseconds }
    }

    /**
     * 获取从现在到之前这段时间内总计禁言时间
     * @param time: 毫秒
     */
    fun getRestrictedDurationWithin(qq: Long?, time: Duration = 7.days): Long {
        val now = System.currentTimeMillis()
        val l = getRestricted(qq)

        return l.filter { now - (it.time ?: 0L) <= time.inWholeMilliseconds }.mapNotNull { it.duration }.sum()
    }

    data class NewbieRestrict(
        var qq: Long? = null,

        var star: Double? = null,

        var time: Long? = null,

        var duration: Long? = null,
    ) {
        fun toLite(): NewbieRestrictLite {
            val r = NewbieRestrictLite()

            r.id = null
            r.qq = qq
            r.star = star
            r.time = time
            r.duration = duration

            return r
        }

        fun fromLite(lite: NewbieRestrictLite): NewbieRestrict {
            qq = lite.qq
            star = lite.star
            time = lite.time
            duration = lite.duration

            return this
        }
    }
}