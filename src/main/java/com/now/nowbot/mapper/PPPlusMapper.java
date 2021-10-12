package com.now.nowbot.mapper;

import com.now.nowbot.entity.MsgLite;
import com.now.nowbot.entity.PPPLite;
import com.now.nowbot.model.PPPlusObject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Map;

public interface PPPlusMapper extends JpaRepository<PPPLite, Long>, JpaSpecificationExecutor<PPPLite> {
    @Query(value = "select new PPPLite(p.userId,p.date,p.Total,p.Junp,p.Flow,p.Acc,p.Sta,p.Spd,p.Pre) from PPPLite p where p.userId = :uid and p.date > :time order by p.date asc",)
    PPPLite getDate(long uid,LocalDateTime time);
//    @Query(value = "select user_id,date,total,jump,flow,acc,sta,spd,pre from osu_pp_plus where user_id = :uid and date > :time order by date asc limit 1", nativeQuery = true)
//    Map<String,Object> getLastDate(long uid,LocalDateTime time);

//    @Query(value = "",nativeQuery = true)
//    PPPlusObject getPPP(long uid);
}
