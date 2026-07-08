package com.now.nowbot.dao;

import com.now.nowbot.config.PermissionType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PermissionDao {
    public List<Long> getQQList(String service, PermissionType type){
        return new ArrayList<Long>();
    }
}
