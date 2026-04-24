package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.dao.UserSnapShotDao
import com.now.nowbot.service.osuApiService.OsuUserApiService
import org.springframework.stereotype.Service

@Service
class BestsHistoryCoverService(
    private val userApiService: OsuUserApiService,
    private val bestSnapShotDao: UserSnapShotDao
) {


}