package com.now.nowbot.mapper;

import com.now.nowbot.entity.bind.QQBindLite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BindQQMapper extends JpaRepository<QQBindLite, Long>, JpaSpecificationExecutor<QQBindLite> {

    @Query("select qq from QQBindLite qb where qb.osuUser.osuID = :osuID")
    Optional<QQBindLite> findByOsuID(Long osuID);

    @Query("select count(o) from OsuBindUserLite o where o.osuID = :osuID")
    int countByOsuID(Long osuID);

    @Modifying
    @Transactional
    @Query("delete from QQBindLite qb where qb.osuUser.osuID = :osuID and qb.qq != :qq")
    int deleteOtherBind(Long osuID, Long qq);

    @Modifying
    @Transactional
    @Query("delete from QQBindLite qb where qb.osuUser.osuID = :osuID")
    void unBind(Long osuID);

    @Query("select qb.qq as qid, qb.osuUser.osuID as uid, qb.osuUser.osuName as name from QQBindLite qb where qb.qq in (:qq)")
    List<QQBindLite.QQUser> findAllUserByQQ(Collection<Long> qq);
}
