package com.now.nowbot.mapper;

import com.now.nowbot.entity.UserLite;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserMapper extends JpaRepository<UserLite, Long> {
}
