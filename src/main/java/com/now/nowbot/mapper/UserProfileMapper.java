package com.now.nowbot.mapper;

import com.now.nowbot.entity.UserProfile;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public interface UserProfileMapper extends JpaRepository<UserProfile, Long> {

    @Cacheable(value = "user_profile", key = "#aLong")
    Optional<UserProfile> findTopByUserId(long aLong);
}
