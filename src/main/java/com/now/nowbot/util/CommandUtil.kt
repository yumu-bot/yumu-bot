package com.now.nowbot.util

import com.now.nowbot.dao.BindDao
import com.now.nowbot.dao.GroupDao
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.service.sbApiService.SBUserApiService
import org.springframework.stereotype.Component

@Component
class CommandUtil(
    private val bindDao: BindDao,
    private val groupDao: GroupDao,
    private val userApiService: OsuUserApiService,
    private val sbUserApiService: SBUserApiService,
) {

}