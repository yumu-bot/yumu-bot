package com.now.nowbot.mapper;

import com.now.nowbot.entity.QQID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.beans.Transient;
import java.util.List;

public interface QQIDMapper extends JpaRepository<QQID, Long>, JpaSpecificationExecutor<QQID> {
    List<QQID> getByPermissionId(Long permissionId);

    @Modifying
    @Transient
    void deleteQQIDByPermissionIdAndIsGroupAndQQ(Long permissionId, Boolean isGroup, Long QQ);

    @Modifying
    @Transient
    void deleteQQIDByPermissionIdAndIsGroup(Long permissionId, Boolean isGroup);

    @Query("select id.QQ from QQID id where id.isGroup=true and id.permissionId=:pid")
    List<Long> getQQIDByPermissionId(Long pid);
}
