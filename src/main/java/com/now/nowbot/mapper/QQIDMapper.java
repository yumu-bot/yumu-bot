package com.now.nowbot.mapper;

import com.now.nowbot.entity.QQID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface QQIDMapper extends JpaRepository<QQID, Long>, JpaSpecificationExecutor<QQID> {
    List<QQID> getByPermissionId(Long permissionId);
    QQID deleteQQIDByPermissionIdAndIsGroupAndQQ(Long permissionId, Boolean isGroup, Long QQ);
}
