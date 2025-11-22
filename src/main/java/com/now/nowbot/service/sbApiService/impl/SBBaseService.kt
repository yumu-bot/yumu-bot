package com.now.nowbot.service.sbApiService.impl

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class SBBaseService(val webClient: WebClient, @param:Qualifier("sbApiWebClient") val sbApiWebClient: WebClient)