package com.now.nowbot.config

import me.aloic.rosupp.AlgorithmVersion
import me.aloic.rosupp.RosuPp
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RosuConfig {

    @Bean
    fun rosuPp(config: OsuLocalCalculateConfig): RosuPp? {
        return if (config.rosu) {
            RosuPp.forVersion(AlgorithmVersion.REWORK_20260706)
        } else {
            null
        }
    }
}