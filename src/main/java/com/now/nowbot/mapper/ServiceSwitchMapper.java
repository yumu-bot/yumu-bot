package com.now.nowbot.mapper;

import com.now.nowbot.entity.ServiceSwitchLite;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceSwitchMapper extends JpaRepository<ServiceSwitchLite, String>{
}
