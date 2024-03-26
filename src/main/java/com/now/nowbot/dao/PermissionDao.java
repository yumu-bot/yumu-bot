package com.now.nowbot.dao;

import com.now.nowbot.config.PermissionType;
import com.now.nowbot.entity.PermissionLite;
import com.now.nowbot.entity.QQID;
import com.now.nowbot.mapper.PermissionMapper;
import com.now.nowbot.mapper.QQIDMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PermissionDao {
    @Resource
    PermissionMapper permissionMapper;
    @Resource
    QQIDMapper qqMapper;

    public List<Long> getQQList(String service, PermissionType type){
        var perm = permissionMapper.getByServiceAndType(service, type);
        if(perm == null){
            perm = permissionMapper.save(new PermissionLite(service, type));
        }
        return qqMapper.getByPermissionId(perm.getId()).stream().map(QQID::getQQ).toList();
    }
    public void addGroup(String service, PermissionType type, Long id){
        Long pid = permissionMapper.getId(service, type);
        var data = new QQID();
        data.setGroup(true);
        data.setPermissionId(pid);
        data.setQQ(id);
        qqMapper.saveAndFlush(data);
    }
    public void deleteGroup(String service, PermissionType type, Long id){
        Long pid = permissionMapper.getId(service, type);
        qqMapper.deleteQQIDByPermissionIdAndIsGroupAndQQ(pid,true,id);
    }

    // QQIDMapper
    public void deleteGroupAll(String service, PermissionType type) {
        // Long pid = permissionMapper.getId(service, type);
        // qqMapper.deleteQQIDByPermissionIdAndIsGroup(pid, true);
    }

    public void addUser(String service, PermissionType type, Long id){
        Long pid = permissionMapper.getId(service, type);
        var data = new QQID();
        data.setGroup(false);
        data.setPermissionId(pid);
        data.setQQ(id);
        qqMapper.saveAndFlush(data);
    }
    public void deleteUser(String service, PermissionType type, Long id){
        Long pid = permissionMapper.getId(service, type);
        qqMapper.deleteQQIDByPermissionIdAndIsGroupAndQQ(pid,false,id);
    }
}
