package com.now.nowbot.mapper;

import com.now.nowbot.entity.DrawLogLite;
import com.now.nowbot.model.DrawConfig;
import com.now.nowbot.model.enums.DrawKind;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public interface DrawLogLiteRepository extends JpaRepository<DrawLogLite, Long> , JpaSpecificationExecutor<DrawLogLite> {

    /***
     * 统计指定范围的品级卡数
     * @param uid 谁
     * @param n 多少抽之内
     * @param kinds 出的品级
     * @return 多少抽之内出的选定品级结果总和
     */
    @Query(value = "select count(*) from (select * from test_draw where uid=:uid order by id desc limit :n) as dlist where dlist.kind in :#{#kinds.![ordinal()]}", nativeQuery = true)
    Integer getKindCount(long uid, int n, @Param("kinds") DrawKind... kinds);

    /***
     * 上一次抽中选定品级的 id,内定方法,外部勿用
     * @param uid 谁
     * @param kinds 品级
     * @return id
     */
    @Query(value = "select id from test_draw where uid=:uid and kind in :#{#kinds.![ordinal()]} order by id desc limit 1", nativeQuery = true)
    Optional<Long> getBeforId(long uid, @Param("kinds") DrawKind... kinds);

    /***
     * 统计抽的次数
     * @param uid
     * @return 次数
     */
    @Query("select count(i) from DrawLogLite i where i.uid=:uid")
    Integer getAllCount(long uid);

    /***
     * 某次结果之后的抽卡次数
     * @param uid 谁
     * @param id 抽卡记录id
     * @return 次数
     */
    @Query(value = "select count(*) from test_draw where uid=:uid and id>=:id", nativeQuery = true)
    Integer getBeforCountById(long uid, long id);

    /***
     * 上次抽到指定品级之后抽了几次
     * @param uid 谁
     * @param kinds 品级
     * @return 次数
     */
    default int getBeforCount(long uid, DrawKind... kinds){
        return getBeforId(uid, kinds).map(aLong -> getBeforCountById(uid, aLong)).orElseGet(() -> getAllCount(uid));
    }

    @Query("select l.kind as kind, l.card as card from DrawLogLite l where l.uid=:uid group by l.card")
    List<Map<String, Object>> _getAllCard(long uid);

    default List<DrawConfig.CardLog> getAllCard(long uid){
        return _getAllCard(uid).stream().map(d -> new DrawConfig.CardLog((DrawKind) d.get("kind"), (String) d.get("card"))).toList();
    }
    @Query("select l.kind as kind, l.card as card from DrawLogLite l where l.uid=:uid and l.kind in :kinds group by l.card")
    List<Map<String, Object>> _getAllCardByKind(long uid, DrawKind... kinds);

    default List<DrawConfig.CardLog> getAllCardByKind(long uid, DrawKind... kinds){
        return _getAllCardByKind(uid, kinds).stream().map(d -> new DrawConfig.CardLog((DrawKind) d.get("kind"), (String) d.get("card"))).toList();
    }
}