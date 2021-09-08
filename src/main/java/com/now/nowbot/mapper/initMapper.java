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

    @Insert("select cn_notebook_id,cn_note_title from cn_note where cn_user_id = #{userId}")
    @Results({
            @Result(property = "id", column = "cn_notebook_id"),
            @Result(property = "title", column = "cn_note_title"),
    })
    public void insert();
    @Select("select * from databases")
    @Results({
            @Result(property = "id", column = "cn_user_id"),
            @Result(property = "name", column = "cn_user_name"),
            @Result(property = "password", column = "cn_user_password"),
            @Result(property = "token", column = "cn_user_token"),
            @Result(property = "nick", column = "cn_user_nick"),
    })
    public Map get();
    @Select("pragma table_info(${name})")
    public List<JSONObject> getlin(String name);
    @Update("CREATE TABLE a(" +
            "id int PRIMARY KEY," +
            "name text"+
            ")")
    public int createa();
    @Update("create table b(id int primary key, name text);create index name on b")
    public int createb();
    @Select("SELECT name FROM sqlite_master")
    public List<JSONObject> getTables();
}
