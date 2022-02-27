package com.now.nowbot.mapper;

import com.now.nowbot.entity.OsuBindUserLite;
import com.now.nowbot.model.enums.OsuMode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


public interface BindMapper extends JpaRepository<OsuBindUserLite, Long>, JpaSpecificationExecutor<OsuBindUserLite> {
    List<OsuBindUserLite> getByQq(Long qq);

    OsuBindUserLite getFirstByQqOrderByTimeDesc(Long qq);

    OsuBindUserLite getByOsuId(Long osuId);

    OsuBindUserLite getByOsuNameLike(String osuName);

    @Modifying
    @Transactional
    void deleteByQqAndIdNot(Long qq, Long id);

    @Modifying
    @Transactional
    @Query("update OsuBindUserLite o set o.accessToken = :accessToken,o.refreshToken = :refreshToken, o.time = :time where osuId=:uid")
    void updateToken(Long uid, String accessToken, String refreshToken, Long time);

    @Modifying
    @Transactional
    @Query("update OsuBindUserLite o set o.mainMode = :mode where osuId = :uid ")
    void updateMode(Long uid, OsuMode mode);

    @Modifying
    @Transactional
    @Query("update OsuBindUserLite o set o.qq = null where osuId = :uid ")
    void unBind(Long uid);

    @Query("select o.qq from OsuBindUserLite o where o.osuId = :uid")
    Long getqq(Long uid);
}
