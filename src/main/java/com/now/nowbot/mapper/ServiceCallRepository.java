package com.now.nowbot.mapper;

import com.now.nowbot.entity.ServiceCallLite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.beans.Transient;
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
            select service, percentile_cont(:limit) within group ( order by time) as data
                    from (
                        select
                            service,
                            time
                        from service_call
                        where ctime between :start and :end
                        order by time
                    ) as rank_data
                    group by service;
            """,
            nativeQuery = true)
    List<ServiceCallLite.ServiceCallResultLimit> countBetweenLimit(LocalDateTime start, LocalDateTime end, Double limit);

    @Transient
    default void saveCall(String service, long time) {
        save(new ServiceCallLite(service, time));
    }
}
