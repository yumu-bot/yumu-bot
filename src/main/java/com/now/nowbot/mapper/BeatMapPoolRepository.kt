package com.now.nowbot.mapper;

import com.now.nowbot.entity.BeatMap4Pool;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface BeatMapPoolMapper extends JpaRepository<BeatMap4Pool, Integer>, JpaSpecificationExecutor<BeatMapMapper> {
    BeatMap4Pool findAllById(int id);
}
