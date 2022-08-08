package com.now.nowbot.mapper;

import com.now.nowbot.entity.MapSetLite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MapSetMapper extends JpaRepository<MapSetLite, Integer>, JpaSpecificationExecutor<MapSetLite> {
    @Override
    void deleteById(Integer integer);
}
