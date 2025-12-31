package com.now.nowbot.dao;

import com.now.nowbot.entity.PermissionLite;
import com.now.nowbot.entity.QQID;
import com.now.nowbot.mapper.PermissionMapper;
import com.now.nowbot.mapper.QQIDMapper;
import com.now.nowbot.permission.PermissionType;
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
        return qqMapper.getByPermissionID(perm.getId()).stream().map(QQID::getQQ).toList();
    }
    public void addGroup(String service, PermissionType type, Long id){
        Long pid = permissionMapper.getId(service, type);
        var data = new QQID();
        data.setGroup(true);
        data.setPermissionID(pid);
        data.setQQ(id);
        qqMapper.saveAndFlush(data);
    }

    public List<Long> queryGroup(String service, PermissionType type) {
        var pid = permissionMapper.getId(service, type);
        return qqMapper.getQQIDByPermissionID(pid);
    }

    public void deleteGroup(String service, PermissionType type, Long id){
        Long pid = permissionMapper.getId(service, type);
        qqMapper.deleteQQIDByPermissionIDAndIsGroupAndQQ(pid,true,id);
    }

    // QQIDMapper
    public void deleteGroupAll(String service, PermissionType type) {
        Long pid = permissionMapper.getId(service, type);
        qqMapper.deleteQQIDByPermissionIDAndIsGroup(pid, true);
    }

    public void addUser(String service, PermissionType type, Long id){
        Long pid = permissionMapper.getId(service, type);
        var data = new QQID();
        data.setGroup(false);
        data.setPermissionID(pid);
        data.setQQ(id);
        qqMapper.saveAndFlush(data);
    }
    public void deleteUser(String service, PermissionType type, Long id){
        Long pid = permissionMapper.getId(service, type);
        qqMapper.deleteQQIDByPermissionIDAndIsGroupAndQQ(pid,false,id);
    }
}
