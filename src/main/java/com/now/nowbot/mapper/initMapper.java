package com.now.nowbot.mapper;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Mapper
@Component
public interface initMapper {

    @Insert("select * from b")
    @Results({
            @Result(property = "id", column = "cn_notebook_id"),
            @Result(property = "title", column = "cn_note_title"),
    })
    public void insert();
    @Insert("insert into b (id, name) VALUES (#{id}, #{name});")
    public int insertb(JSONObject date);
    @Select("pragma table_info(${name})")
    public List<JSONObject> getlin(String name);
    @Update("CREATE TABLE a(" +
            "id int PRIMARY KEY," +
            "name text"+
            ")")
    public int createa();
    @Update("create table b(id int primary key, name text);create index name on b (name)")
    public int createb();
    @Select("SELECT name FROM sqlite_master")
    public List<JSONObject> getTables();
}
