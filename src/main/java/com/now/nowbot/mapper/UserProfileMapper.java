package com.now.nowbot.mapper;

import com.now.nowbot.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserProfileMapper extends JpaRepository<UserProfile, Long> {

    Optional<UserProfile> findTopByUserId(long aLong);
}
