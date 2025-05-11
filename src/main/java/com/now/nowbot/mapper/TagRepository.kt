package com.now.nowbot.mapper

import com.now.nowbot.entity.TagLite
import org.springframework.data.jpa.repository.JpaRepository

interface TagRepository : JpaRepository<TagLite, Int>