package com.now.nowbot.mapper;

import com.now.nowbot.entity.QQID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QQIDMapper extends JpaRepository<QQID, Long>, JpaSpecificationExecutor<QQID> {
    public List<Long> getByPermissionId(Long permissionId);
    public QQID deleteQQIDByPermissionIdAndIsGroupAndQQ(Long permissionId, Boolean isGroup, Long QQ);
}
