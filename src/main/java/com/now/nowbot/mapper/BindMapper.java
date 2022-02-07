package com.now.nowbot.mapper;

import com.now.nowbot.entity.QQUserLite;
import com.now.nowbot.model.enums.OsuMode;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;


public interface BindMapper extends JpaRepository<QQUserLite, Long>, JpaSpecificationExecutor<QQUserLite> {
    QQUserLite getByQq(Long qq);

    QQUserLite getByOsuId(Long osuId);

    QQUserLite getByOsuNameLike(String osuName);

    @Modifying
    @Transactional
    @Query("update QQUserLite o set o.accessToken = ?2,o.refreshToken = ?3 where osuId=?1")
    void updateToken(Long uid, String accessToken, String refreshToken);

    @Modifying
    @Transactional
    @Query("update QQUserLite o set o.mainMode = :mode where osuId = :uid ")
    void updateMode(Long uid, OsuMode mode);

    @Modifying
    @Transactional
    @Query("update QQUserLite o set o.qq = null where osuId = :uid ")
    void unBind(Long uid);

    @Query("select o.qq from QQUserLite o where o.osuId = :uid")
    Long getqq(Long uid);
}
