package com.now.nowbot.image.resource

import org.springframework.http.CacheControl
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController

object RenderResourcePaths {
    const val ENDPOINT = "/render-resource"
}

@RestController
@RequestMapping(value = [RenderResourcePaths.ENDPOINT], method = [RequestMethod.GET])
class RenderResourceController(
    private val service: RenderResourceService,
) {
    @GetMapping("/{id}")
    fun get(@PathVariable("id") id: String): ResponseEntity<ByteArray> {
        val resource = service.open(id) ?: return ResponseEntity.notFound().build<ByteArray>()
        return ResponseEntity.ok()
            .contentType(resource.mediaType)
            .contentLength(resource.bytes.size.toLong())
            .cacheControl(CacheControl.noStore())
            .header("Access-Control-Allow-Origin", "*")
            .header("X-Content-Type-Options", "nosniff")
            .header("Content-Security-Policy", CSP)
            .body(resource.bytes)
    }

    companion object {
        private const val CSP =
            "default-src 'none'; img-src data:; style-src 'unsafe-inline'; " +
                "font-src data:; frame-ancestors 'none'; sandbox"
    }
}
