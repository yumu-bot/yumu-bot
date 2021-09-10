package com.now.nowbot.mapper;

import com.alibaba.fastjson.JSONObject;
import com.now.nowbot.entity.MsgLite;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Component;

import java.util.List;

@Mapper
@Component
public interface MessageMapper {
    @Update("create table qq_message(id integer,internal integer,time integer,from_id integer, target_id integer,msg text);\n" +
            "create index msg_index on qq_message (id, internal, time);")
    int init();

    @Insert("insert into qq_message (id, internal, time, from_id, target_id, msg) values (#{id}, #{internal}, #{time}, #{from_id}, #{target_id}, #{msg});")
    int addMsg(MsgLite msgLite);

    @Select("select target_id as group , count(*) as msgs from qq_message group by (target_id) order by count(*) desc ;")
    List<JSONObject> contGroup();

    @Select("select from_id as send , count(*) as msgs from qq_message where target_id=#{group} group by (from_id) order by count(*) desc ;")
    List<JSONObject> contSend(long group);

    @Select("select msg from qq_message where target_id = #{target}")
    List<String> queryQQ(long target);

    @Delete("delete from qq_message where target=#{qq}")
    int delQQ(long qq);

    @Select("select msg from qq_message where id=#{id} and internal=#{internal} and time=#{time} limit 1;")
    String findMsg(int id, int internal, int time);
}
