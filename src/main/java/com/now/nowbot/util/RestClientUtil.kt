package com.now.nowbot.util

import com.now.nowbot.util.DataUtil.findCauseOfType
import org.springframework.web.client.HttpClientErrorException
import java.io.IOException

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
    val jsonString = try {
        exchange { _, response ->
            if (response.statusCode.is4xxClientError || response.statusCode.is5xxServerError) {
                throw HttpClientErrorException(response.statusCode, response.statusText, response.body.readAllBytes(), null)
            } else {
                String(response.body.readAllBytes(), Charsets.UTF_8)
            }
        }
    } catch (e: Exception) {
        if (e.findCauseOfType<java.net.SocketTimeoutException>() != null ||
            e.findCauseOfType<java.net.SocketException>() != null
        ) {
            throw org.springframework.web.client.HttpServerErrorException(
                org.springframework.http.HttpStatus.REQUEST_TIMEOUT,
                "Request Timeout 请求超时"
            )
        } else if (
            e.findCauseOfType<IOException>() != null ||
            e.findCauseOfType<java.net.ConnectException>() != null
        ) {
            throw org.springframework.web.client.HttpServerErrorException(
                org.springframework.http.HttpStatus.GATEWAY_TIMEOUT,
                "Gateway Timeout 网关超时"
            )
        } else {
            throw e
        }
    }

    if (T::class == String::class) {
        return jsonString as T
    }

    return JacksonUtil.parseObject(jsonString)!!
}

inline fun <reified T : Any> org.springframework.web.client.RestClient.RequestHeadersSpec<*>.toBodyList(): List<T> {

    val jsonString = try {
        exchange { _, response ->
            if (response.statusCode.is4xxClientError || response.statusCode.is5xxServerError) {
                // 注意：Spring 6+ 建议使用 HttpStatusCodeException 的子类
                throw HttpClientErrorException(response.statusCode, response.statusText, response.body.readAllBytes(), null)
            } else {
                String(response.body.readAllBytes(), Charsets.UTF_8)
            }
        }
    } catch (e: Exception) {
        if (e.findCauseOfType<java.net.SocketTimeoutException>() != null ||
            e.findCauseOfType<java.net.SocketException>() != null
        ) {
            throw org.springframework.web.client.HttpServerErrorException(
                org.springframework.http.HttpStatus.REQUEST_TIMEOUT,
                "408 Request Timeout"
            )
        } else if (
            e.findCauseOfType<IOException>() != null ||
            e.findCauseOfType<java.net.ConnectException>() != null
        ) {
            throw org.springframework.web.client.HttpServerErrorException(
                org.springframework.http.HttpStatus.GATEWAY_TIMEOUT,
                "504 Gateway Timeout"
            )
        } else {
            throw e
        }
    }

    return JacksonUtil.parseObjectList(jsonString, T::class.java)
}