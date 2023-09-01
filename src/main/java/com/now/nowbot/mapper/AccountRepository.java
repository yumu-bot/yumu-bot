package com.now.nowbot.mapper;

import com.now.nowbot.entity.UserAccountLite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface AccountRepository extends JpaRepository<UserAccountLite, Long>, JpaSpecificationExecutor<UserAccountLite> {
    @Query(value = "select * from account limit 1 offset :index", nativeQuery = true)
    UserAccountLite getByIndex(long index);
}
