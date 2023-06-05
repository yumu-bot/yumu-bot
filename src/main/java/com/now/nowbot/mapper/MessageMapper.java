package com.now.nowbot.mapper;

import com.now.nowbot.entity.MsgLite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface MessageMapper extends JpaRepository<MsgLite, Long> , JpaSpecificationExecutor<MsgLite> {
    /**
     * 群聊消息统计
     * @return
     */
    @Query(value = "select target_id as groupid , count(target_id)  as msgs from qq_message group by (target_id) order by count(target_id) desc", nativeQuery = true)
    public List<Map<String, Number>> contGroup();
    @Query(value = "select target_id as groupid , count(target_id)  as msgs from qq_message where time > :start and time< :end group by (target_id) order by count(target_id) desc", nativeQuery = true)
    public List<Map<String, Number>> contGroup(@Param("start") int start, @Param("end") int end);
    @Query(value = "select from_id as qq , count(from_id) as sum from qq_message where time > :start and time< :end and target_id= :groupid and from_id in (:ids) group by (from_id) order by sum desc", nativeQuery = true)
    public List<Map<String, Number>> contGroupSender(@Param("start") long start, @Param("end") long end, @Param("groupid")long group, @Param("ids")long... ids);

    public List<MsgLite> getAllByTargetIdAndTimeBetween(Long targetId, Long start_time, Long end_time);

    public MsgLite getByRawIdAndInternalAndFromId(int rawId, int internal, Long fromId);

    public MsgLite getAllByRawIdAndInternalAndTime(Integer rawId, Integer internal, Long time);
//
//    @Query("select from_id as senderid , count(from_id) as msgs from qq_message where target_id=#{groupID} group by (from_id) order by count(from_id) desc")
//    public List<JSONObject> contGroupSender(long groupID);
//    @Query("select from_id as senderid , count(from_id) as msgs from qq_message where target_id=#{groupID} and time>#{start} and time<#{end} group by (from_id) order by count(from_id) desc")
//    public List<JSONObject> contGroupSender(long groupID, int start, int end);

}
