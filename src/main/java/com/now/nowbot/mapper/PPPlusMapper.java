package com.now.nowbot.mapper;

import com.now.nowbot.entity.PPPLite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;

public interface PPPlusMapper extends JpaRepository<PPPLite, Long>, JpaSpecificationExecutor<PPPLite> {
    PPPLite getFirstByUserIdAndDateIsAfterOrderByDateAsc(Long uid, LocalDateTime time);

    PPPLite getFirstByUserIdOrderByDateDesc(long uid);
}
