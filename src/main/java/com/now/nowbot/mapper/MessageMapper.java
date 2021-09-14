package com.now.nowbot.mapper;

import com.now.nowbot.entity.MsgLite;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageMapper extends JpaRepository<MsgLite, Long> {
}
