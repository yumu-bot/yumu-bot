package com.now.nowbot.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Primary
import org.springframework.validation.annotation.Validated

@Primary
@Validated
@ConfigurationProperties(prefix = "yumu.group", ignoreInvalidFields = true)
class NewbieConfig {
    var killerGroup: Long = -1L

    var newbieGroup: Long = -1L

    var advancedGroup: Long = -1L

    var hyperGroup: Long = -1L

    var yumuBot: Long = -1L

    var hydrantBot: Long = -1L

    var remitBIDs: LongArray = longArrayOf()
}