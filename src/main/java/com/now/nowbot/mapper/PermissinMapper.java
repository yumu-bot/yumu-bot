package com.now.nowbot.mapper;

import com.now.nowbot.config.Permission;
import com.now.nowbot.entity.MsgLite;
import com.now.nowbot.entity.PermissionLite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PermissinMapper extends JpaRepository<PermissionLite, Long>, JpaSpecificationExecutor<PermissionLite> {
    public PermissionLite getByServiceAndType(String service, Permission.TYPE type);
}
