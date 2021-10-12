package com.now.nowbot.mapper;

import com.now.nowbot.entity.MsgLite;
import com.now.nowbot.entity.PPPLite;
import com.now.nowbot.model.PPPlusObject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Map;

public interface PPPlusMapper extends JpaRepository<PPPLite, Long>, JpaSpecificationExecutor<PPPLite> {
    PPPLite getFirstByUserIdAndDateIsAfterOrderByDateAsc(Long uid, LocalDateTime time);

    PPPLite getFirstByUserIdOrderByDateDesc(long uid);
}
