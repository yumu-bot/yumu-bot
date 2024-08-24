package com.now.nowbot.qq.tencent

import com.now.nowbot.permission.PermissionImplement
import com.now.nowbot.service.OsuApiService.OsuUserApiService
import com.yumu.YumuService
import com.yumu.model.packages.Command
import com.yumu.model.packages.QueryName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration.Companion.seconds

object YumuServer : YumuService {
    lateinit var userApiService: OsuUserApiService

    override suspend fun onCommand(param: Command.Request): Command.Response {
        val channel = Channel<Command.Response>(0, BufferOverflow.DROP_LATEST)
        val scope = CoroutineScope(coroutineContext)
        val contact = Contact(param.uid) {
            scope.launch { channel.send(it) }
        }
        val event = Event(contact, param.command)
        PermissionImplement.onTencentMessage(event)
        return withTimeout(5.seconds) {
            channel.receive()
        }
    }

    override suspend fun onQueryName(param: QueryName.Request): QueryName.Response? {
        val userID = userApiService.getOsuId(param.name) ?: -1
        return QueryName.Response(param.name, userID)
    }
}