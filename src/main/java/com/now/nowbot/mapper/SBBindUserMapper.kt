package com.now.nowbot.mapper

import com.now.nowbot.entity.SBBindUserLite
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional

interface SBBindUserMapper: JpaRepository<SBBindUserLite, Long>, JpaSpecificationExecutor<SBBindUserLite> {
    @Query("select u from SBBindUserLite u where u.userID = :userID order by u.id limit 1")
    fun getUser(userID: Long): SBBindUserLite?

    @Query(
        """
        select u from SBBindUserLite u
        where u.userID = :userID
        order by u.id desc limit 1
        """
    ) fun getFirstByUserID(userID: Long?): SBBindUserLite?

    @Modifying @Transactional @Query("update SBBindUserLite u set u.modeValue = :modeValue where u.userID = :userID ")
    fun updateMode(userID: Long, modeValue: Byte)

    @Modifying @Transactional @Query("delete from SBBindUserLite u where u.userID = :userID ")
    fun deleteUser(userID: Long)

    @Modifying @Transactional @Query(
        value = """
            with del as (
                select id, row_number() over (partition by user_id = :userID order by id desc ) as row_num
                from sb_bind_user
                where user_id = :userID
            )
            delete from sb_bind_user using del where sb_bind_user.id = del.id and del.row_num >1;
            """, nativeQuery = true)
    fun deleteOutdatedBind(userID: Long?)

    fun countAllByUserID(userID: Long): Int
}