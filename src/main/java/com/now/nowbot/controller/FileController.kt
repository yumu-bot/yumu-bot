package com.now.nowbot.controller

import com.fasterxml.jackson.databind.JsonNode
import com.now.nowbot.config.BeatmapMirrorConfig
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.reactive.function.client.WebClient

@RestController @RequestMapping(value = ["/pub"], method = [RequestMethod.GET])
@CrossOrigin("http://localhost:5173", "https://siyuyuko.github.io")
class FileController(private val webClient: WebClient, mirrorConfig: BeatmapMirrorConfig) {
    private final val token: String? = mirrorConfig.token
    private final val url = mirrorConfig.url

    @GetMapping("/api/{type}/{bid}") fun download(
        @PathVariable("type") type: String, @PathVariable("bid") bid: Long?
    ): ResponseEntity<*> {

        return when(type) {
            "info" -> {
                val data: JsonNode = try {
                    webClient.get().uri("${url}/api/map/getBeatMapInfo/${bid}").headers {
                        it.addIfAbsent("AuthorizationX", token)
                    }.retrieve().bodyToMono(JsonNode::class.java).block()!!
                } catch (e: Exception) {
                    return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON)
                        .body(mapOf("code" to 400, "message" to e.message))
                }

                ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(data)
            }

            "background" -> {
                val data: ByteArray = try {
                    webClient.get().uri(
                        "${url}/api/file/map/bg/${bid}"
                    ).headers {
                        it.addIfAbsent("AuthorizationX", token)
                    }.retrieve().bodyToMono(ByteArray::class.java).block()!!
                } catch (e: Exception) {
                    return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON)
                        .body(mapOf("code" to 400, "message" to e.message))
                }

                ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(data)
            }

            else -> ResponseEntity.status(400).build<Any>()
        }
    }

    private fun ref(name: String, file: MultipartFile) {
        println("${name}'s size is ${file.size}")
    }
}
