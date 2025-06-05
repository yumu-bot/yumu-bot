package com.now.nowbot.mapper;

import com.now.nowbot.entity.bind.DiscordBindLite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface BindDiscordMapper extends JpaRepository<DiscordBindLite, String>, JpaSpecificationExecutor<DiscordBindLite> {
    @Modifying
    @Transactional
    @Query("delete from DiscordBindLite dc where dc.osuUser.osuID=:osuID and dc.id = :id")
    void unBind(String discordID) ;
    @Modifying
    @Transactional
    @Query("delete from DiscordBindLite dc where dc.osuUser.osuID=:osuID")
    void unBind(Long osuID);
}
