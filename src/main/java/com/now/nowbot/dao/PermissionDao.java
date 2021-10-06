package com.now.nowbot.dao;

import com.now.nowbot.config.Permission;
import com.now.nowbot.entity.PermissionLite;
import com.now.nowbot.mapper.PermissionMapper;
import com.now.nowbot.mapper.QQIDMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PermissionDao {
    @Autowired
    PermissionMapper permission;
    @Autowired
    QQIDMapper qqMapper;

    public List<Long> getQQList(String service, Permission.TYPE type){
        var perm = permission.getByServiceAndType(service, type);
        if(perm == null){
            perm = permission.save(new PermissionLite(service, type));
        }
        return qqMapper.getByPermissionId(perm.getId());
    }
}
