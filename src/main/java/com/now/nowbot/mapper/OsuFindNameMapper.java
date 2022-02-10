package com.now.nowbot.mapper;

import com.now.nowbot.entity.OsuNameToIdLite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface OsuFindNameMapper extends JpaRepository<OsuNameToIdLite, Long>, JpaSpecificationExecutor<OsuNameToIdLite> {
    @Override
    <S extends OsuNameToIdLite> List<S> saveAll(Iterable<S> iterable);

    @Modifying
    @Transactional
    @Query("delete from OsuNameToIdLite o where o.name = :name")
    void deleteByName(String name);

    @Modifying
    @Transactional
    @Query("delete from OsuNameToIdLite o where o.uid = :uid")
    void deleteByUid(Long uid);


    OsuNameToIdLite getFirstByNameOrderByIndex(String name);
}
