package com.now.nowbot.mapper;

import com.now.nowbot.entity.PermissionLite;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionMapper extends JpaRepository<PermissionLite, Long> {
}
