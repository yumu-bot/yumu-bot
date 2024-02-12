package com.now.nowbot.mapper;

import com.now.nowbot.entity.ServiceCallLite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface ServiceCallRepository extends JpaRepository<ServiceCallLite, Long> {
    @Query(value = "select service, count(service) as size, avg(time) as avgTime, min(time) as minTime, max(time) as maxTime" +
            " from service_call group by service order by size desc;", nativeQuery = true)
    List<ServiceCallLite.ServiceCallResult> countAll();

    @Query(value = """
            select service, count(service) as size, avg(time) as avgTime, min(time) as minTime, max(time) as maxTime
            from service_call where ctime between :start and :end group by service order by size desc;
            """,
            nativeQuery = true)
    List<ServiceCallLite.ServiceCallResult> countBetween(LocalDateTime start, LocalDateTime end);

    @Query(value = """
            select service, time as data
            from (
                select
                    service,
                    time,
                    row_number() over (partition by service order by time desc ) as row_num,
                    count(*) over (partition by service) as total
                from service_call
                where ctime between :start and :end
            ) as rank_data
            where rank_data.row_num <= floor(0.8 * total)
            order by row_num desc limit 1;
            """,
            nativeQuery = true)
    List<ServiceCallLite.ServiceCallResult$80> countBetween$80(LocalDateTime start, LocalDateTime end);

    default void saveCall(String service, long time) {
        save(new ServiceCallLite(service, time));
    }
}
