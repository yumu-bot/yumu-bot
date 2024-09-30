package com.now.nowbot.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.util.*

@Component
@Configuration
class WebConfig : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(BotWebInterceptor())
    }

    class BotWebInterceptor : HandlerInterceptor {
        private val interceptor = mutableMapOf<String, LinkedList<Long>>()

        override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
            if (handler !is HandlerMethod) return true
            val limitList = interceptor.getOrPut(request.remoteAddr) { LinkedList() }

            val now = System.currentTimeMillis()

            if (limitList.size < 10) {
                limitList.addFirst(now)
                return true
            } else {
                val old = limitList.pollLast()
                limitList.addFirst(now)
                return now - old >= 1000
            }
        }

        override fun afterCompletion(
            request: HttpServletRequest,
            response: HttpServletResponse,
            handler: Any,
            ex: Exception?
        ) {
            interceptor.entries.removeIf {
                it.value.first < System.currentTimeMillis() - 60000
            }
        }
    }
}