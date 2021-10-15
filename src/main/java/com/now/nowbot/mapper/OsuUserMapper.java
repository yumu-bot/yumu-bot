package com.now.nowbot.mapper;

import com.now.nowbot.entity.OsuUserLite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface OsuUserMapper  extends JpaRepository<OsuUserLite, Long>, JpaSpecificationExecutor<OsuUserLite> {
    public OsuUserLite getByOsuID(Long id);
}
