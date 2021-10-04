package com.now.nowbot.mapper;


import com.now.nowbot.config.Permission;
import com.now.nowbot.entity.PermissionLite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface PermissionMapper extends JpaRepository<PermissionLite, Long> , JpaSpecificationExecutor<PermissionLite> {

    public List<PermissionLite> getAllByServiceAndType(String service, Permission.TYPE type);
}
