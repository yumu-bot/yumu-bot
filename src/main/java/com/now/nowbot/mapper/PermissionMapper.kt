package com.now.nowbot.mapper;


import com.now.nowbot.entity.PermissionLite;
import com.now.nowbot.permission.PermissionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PermissionMapper extends JpaRepository<PermissionLite, Long> , JpaSpecificationExecutor<PermissionLite> {
    @Query("select p.id from PermissionLite p where p.service = :service and p.type = :type")
    Long getId(@Param("service") String service, @Param("type") PermissionType type);

    PermissionLite getByServiceAndType(String service, PermissionType type);
}
