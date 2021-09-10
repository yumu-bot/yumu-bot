package com.now.nowbot.mapper;

import com.now.nowbot.config.Permission;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Component;

import java.util.List;

@Mapper
@Component
public interface PermissionMapper {
    @Update("create table permission (servicename text ,type text,checkid integer)")
    int init();

    @Select("select checkid from permission where servicename=#{name} and type=#{type}")
    List<Long> getPerm(String name, Permission.TYPE type);

    @Insert("insert into permission (servicename, type, checkid) values (#{name},#{type},#{id})")
    int addPerm(String name, Permission.TYPE type, Long id);

    @Delete("delete from permission where servicename=#{name} and type=#{type} and checkid = #{id}")
    int delPerm(String name, Permission.TYPE type, Long id);

    @Select("select checkid from permission where type='super'")
    List<Long> getSupper();
}

