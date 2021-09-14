package com.now.nowbot.mapper;

import com.alibaba.fastjson.JSONObject;
import com.now.nowbot.entity.MsgLite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MessageMapper extends JpaRepository<MsgLite, Long> {
    @Query("select target_id as groupid , count(target_id)  as msgs from qq_message group by (target_id) order by count(target_id) desc")
    public List<JSONObject> contGroup();
    @Query("select target_id as groupid , count(target_id)  as msgs from qq_message where time>#{start} and time<#{end} group by (target_id) order by count(target_id) desc")
    public List<JSONObject> contGroup(int start, int end);

    @Query("select from_id as senderid , count(from_id) as msgs from qq_message where target_id=#{groupID} group by (from_id) order by count(from_id) desc")
    public List<JSONObject> contGroupSender(long groupID);
    @Query("select from_id as senderid , count(from_id) as msgs from qq_message where target_id=#{groupID} and time>#{start} and time<#{end} group by (from_id) order by count(from_id) desc")
    public List<JSONObject> contGroupSender(long groupID, int start, int end);

}
