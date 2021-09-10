package com.now.nowbot.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Component;

@Mapper
@Component
public interface MessageMapper {
    @Update("create table qq_message(id integer,internal integer,time integer,source_id integer,msg text);\n" +
            "create index msg_index on qq_message (id, internal, time);")
    int init();

    @Insert("insert into qq_message (id, internal, time, msg) values (#{id}, #{internal}, #{time}, #{source}, #{msg});")
    int addMsg(int id, int internal, int time, long source, String msg);


}
