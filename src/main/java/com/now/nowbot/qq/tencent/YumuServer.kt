package com.now.nowbot.qq.tencent

import com.now.nowbot.permission.PermissionImplement
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
    override suspend fun onCommand(param: Command.Request): Command.Response? {
        val channel = Channel<Command.Response>(0, BufferOverflow.DROP_LATEST)
        val scope = CoroutineScope(coroutineContext)
        val contact = Contact(param.uid) {
            scope.launch { channel.send(it) }
        }
        val event = Event(contact, param.command)
        PermissionImplement.onTencentMessage(event)
        return withTimeout(30.seconds) {
            channel.receive()
        }
    }

    override suspend fun onQueryName(param: QueryName.Request): QueryName.Response? {
        return null
    }
}