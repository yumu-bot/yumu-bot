package com.now.nowbot.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

import java.nio.file.Path

@Validated
@ConfigurationProperties(prefix = "yumu.maimai", ignoreInvalidFields = true)
class DivingFishConfig {
    /**
     * 接口路径, 一般不用改
     */
    var url: String? = "https://www.diving-fish.com/"

    var token: String? = "BMurCyOaA0cfist6VpNvb7ZXK5h1noSE"

    // D:/App2/[Projects]/yumu-bot-run/img/ExportFileV3/Maimai
    // /home/spring/work/img/ExportFileV3/Maimai
    var maimai: Path? = Path.of("/home/spring/work/img/ExportFileV3/Maimai")

    // D:/App2/[Projects]/yumu-bot-run/img/ExportFileV3/Chunithm
    // /home/spring/work/img/ExportFileV3/Chunithm
    var chunithm: Path? = Path.of("/home/spring/work/img/ExportFileV3/Chunithm")
}
