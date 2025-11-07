package com.now.nowbot.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated
import java.nio.file.Path

@Validated
@ConfigurationProperties(prefix = "yumu.lxns", ignoreInvalidFields = true)
class LxnsConfig {
    /**
     * 接口路径, 一般不用改
     */
    var url: String? = "https://maimai.lxns.net"

    var assetHost: String? = "assets2.lxns.net"

    /**
     * 开发者 token，自己申请
     */
    var token: String? = ""

    // D:/App2/[Projects]/yumu-bot-run/img/ExportFileV3/Maimai
    // /home/spring/work/img/ExportFileV3/Maimai
    var maimai: Path? = Path.of("/home/spring/work/img/ExportFileV3/Maimai")

    // D:/App2/[Projects]/yumu-bot-run/img/ExportFileV3/Chunithm
    // /home/spring/work/img/ExportFileV3/Chunithm
    var chunithm: Path? = Path.of("/home/spring/work/img/ExportFileV3/Chunithm")
}