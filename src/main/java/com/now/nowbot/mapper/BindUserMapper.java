package com.now.nowbot.mapper;

import com.now.nowbot.entity.OsuBindUserLite;
import com.now.nowbot.model.enums.OsuMode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;


public interface BindUserMapper extends JpaRepository<OsuBindUserLite, Long>, JpaSpecificationExecutor<OsuBindUserLite> {
    Optional<OsuBindUserLite> getByOsuId(Long osuId);

    Optional<OsuBindUserLite> getByOsuNameLike(String osuName);

    @Modifying
    @Transactional
    @Query("update OsuBindUserLite o set o.accessToken = :accessToken,o.refreshToken = :refreshToken, o.time = :time where o.osuId=:uid")
    void updateToken(Long uid, String accessToken, String refreshToken, Long time);

    @Modifying
    @Transactional
    @Query("update OsuBindUserLite o set o.mainMode = :mode where o.osuId = :uid ")
    void updateMode(Long uid, OsuMode mode);


    @Modifying
    @Transactional
    @Query("delete OsuBindUserLite o where o.osuId = :uid ")
    void deleteAllByOsuId(Long uid);
}
