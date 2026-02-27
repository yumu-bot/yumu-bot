package com.now.nowbot.service.sbApiService.impl

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

@Service
class SBBaseService(
    @param:Qualifier("sbApiRestClient")
    val sbApiRestClient: RestClient
)