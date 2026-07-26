package com.now.nowbot.dao

import com.now.nowbot.entity.OsuGroupConfigLite
import com.now.nowbot.mapper.OsuGroupConfigRepository
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.enums.OsuMode.Companion.isDefaultOrNull
import com.now.nowbot.qq.contact.Group
import com.now.nowbot.qq.event.MessageEvent
import org.springframework.stereotype.Service
import kotlin.jvm.optionals.getOrNull

@Service
class GroupDao(private val repository: OsuGroupConfigRepository) {

    val allGroupMode: Map<Long, OsuMode>
        get() = repository.findAll().associate {
            (it.groupID ?: -1) to (it.mode ?: OsuMode.DEFAULT)
        }

    fun getGroupMode(event: MessageEvent?): OsuMode {
        val groupID = (event?.subject as? Group)?.contactID ?: return OsuMode.DEFAULT
        return repository.findById(groupID).getOrNull()?.mode ?: OsuMode.DEFAULT
    }

    fun saveGroupMode(groupID: Long, mode: OsuMode?) {
        if (mode.isDefaultOrNull()) {
            repository.deleteById(groupID)
        } else {
            repository.save(OsuGroupConfigLite(groupID, mode))
        }
    }
}