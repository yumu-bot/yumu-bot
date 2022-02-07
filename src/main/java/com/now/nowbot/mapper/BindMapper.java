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
    @Query("update QQUserLite o set o.accessToken = :accessToken,o.refreshToken = :refreshToken, o.time = :time where osuId=:uid")
    void updateToken(Long uid, String accessToken, String refreshToken, Long time);

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
