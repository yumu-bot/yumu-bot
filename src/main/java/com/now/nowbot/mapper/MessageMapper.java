package com.now.nowbot.mapper;

import com.alibaba.fastjson.JSONObject;
import com.now.nowbot.entity.MsgLite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;

public interface MessageMapper extends JpaRepository<MsgLite, Long> , JpaSpecificationExecutor<MsgLite> {
    @Query(value = "select target_id as groupid , count(target_id)  as msgs from qq_message group by (target_id) order by count(target_id) desc", nativeQuery = true)
    public List<Map<String, Long>> contGroup();
    @Query(value = "select target_id as groupid , count(target_id)  as msgs from qq_message where time > :start and time< :end group by (target_id) order by count(target_id) desc", nativeQuery = true)
    public List<Map<String, Long>> contGroup(@Param("start") int start, @Param("end") int end);

    public List<MsgLite> getAllByTargetIdAndTimeBetween(Long targetId, Long start_time, Long end_time);

    public MsgLite getAllByIdAndRawIdAndAndInternal(Long id, Integer rawId, Integer internal);
//
//    @Query("select from_id as senderid , count(from_id) as msgs from qq_message where target_id=#{groupID} group by (from_id) order by count(from_id) desc")
//    public List<JSONObject> contGroupSender(long groupID);
//    @Query("select from_id as senderid , count(from_id) as msgs from qq_message where target_id=#{groupID} and time>#{start} and time<#{end} group by (from_id) order by count(from_id) desc")
//    public List<JSONObject> contGroupSender(long groupID, int start, int end);

}
