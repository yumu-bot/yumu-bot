package com.now.nowbot.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Component;

import java.util.Map;

@Mapper
@Component
public interface QQmapper {
    String[] TABLE_NAME= {"QQ", "OSUID", ""};
}
