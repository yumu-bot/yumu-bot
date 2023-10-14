package com.now.nowbot.mapper;

import com.now.nowbot.entity.bind.QQBindLite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface BindQQMapper extends JpaRepository<QQBindLite, Long>, JpaSpecificationExecutor<QQBindLite> {

    @Query("select qq from QQBindLite qq where qq.osuUser.osuId = :osuId")
    Optional<QQBindLite> findByOsuId(Long osuId);

    @Modifying
    @Transactional
    @Query("delete from QQBindLite qq where qq.osuUser.osuId=:osuId and qq.qq != :saveQQ")
    int deleteOtherBind(Long osuId, Long saveQQ);
    void deleteByQq(Long qq);
    @Modifying
    @Transactional
    @Query("delete from QQBindLite qq where qq.osuUser.osuId=:osuId")
    void unBind(Long osuId);
}
