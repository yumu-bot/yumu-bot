package com.now.nowbot.util

import org.springframework.web.client.HttpClientErrorException

inline fun <reified T : Any> org.springframework.web.client.RestClient.RequestHeadersSpec<*>.toBody(): T {
    if (T::class == ByteArray::class) {
        return exchange { _, response ->
            if (response.statusCode.is4xxClientError || response.statusCode.is5xxServerError) {
                throw HttpClientErrorException(
                    response.statusCode,
                    response.statusText,
                    response.body.readAllBytes(),
                    null
                )
            } else {
                response.body.readAllBytes()
            }
        } as T
    }
    val jsonString = exchange { _, response ->
        if (response.statusCode.is4xxClientError || response.statusCode.is5xxServerError) {
            throw HttpClientErrorException(response.statusCode, response.statusText, response.body.readAllBytes(), null)
        } else {
            String(response.body.readAllBytes(), Charsets.UTF_8)
        }
    }

    if (T::class == String::class) {
        return jsonString as T
    }

    return JacksonUtil.parseObject(jsonString, T::class.java)
}

inline fun <reified T : Any> org.springframework.web.client.RestClient.RequestHeadersSpec<*>.toBodyList(): List<T> {

    val jsonString = exchange { _, response ->
        if (response.statusCode.is4xxClientError || response.statusCode.is5xxServerError) {
            throw HttpClientErrorException(response.statusCode, response.statusText, response.body.readAllBytes(), null)
        } else {
            String(response.body.readAllBytes(), Charsets.UTF_8)
        }
    }

    return JacksonUtil.parseObjectList(jsonString, T::class.java)
}