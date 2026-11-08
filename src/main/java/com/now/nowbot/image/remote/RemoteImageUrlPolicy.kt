package com.now.nowbot.image.remote

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

internal class RemoteImageUrlPolicy {
    fun parse(value: String): HttpUrl? = value.trim().toHttpUrlOrNull()

    fun resolve(base: HttpUrl, location: String): HttpUrl? = base.resolve(location.trim())
}
