package com.now.nowbot.dao;

import com.now.nowbot.mapper.PPPlusMapper;
import com.now.nowbot.model.PPPlusObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
public class PPPlusDao {
    @Autowired
    PPPlusMapper ppPlusMapper;

    public PPPlusObject getobject(long uid){

    }
}
