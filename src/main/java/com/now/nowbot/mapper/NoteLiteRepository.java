package com.now.nowbot.mapper;

import com.now.nowbot.entity.NoteLite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface NoteLiteRepository extends JpaRepository<NoteLite, Long>, JpaSpecificationExecutor<NoteLite> {
    @Query("select e from NoteLite e where e.corn is not null")
    List<NoteLite> getCornList();

    @Query("select e from NoteLite e where e.index like '%:k%'")
    List<NoteLite> searchNoteLiteByIndex(String k);
}