package com.now.nowbot.controller

import tools.jackson.databind.JsonNode
import com.now.nowbot.config.BeatmapMirrorConfig
import com.now.nowbot.util.toBody
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.RestClient
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping(value = ["/pub"], method = [RequestMethod.GET])
@CrossOrigin("http://localhost:5173", "https://siyuyuko.github.io")
class FileController(
    @field:Qualifier("restClient")
    private val restClient: RestClient,
    mirrorConfig: BeatmapMirrorConfig
) {
    private final val token: String? = mirrorConfig.token
    private final val url = mirrorConfig.url

    @GetMapping("/api/{type}/{bid}")
    fun download(
        @PathVariable("type") type: String, @PathVariable("bid") bid: Long?
    ): ResponseEntity<*> {

        return when (type) {
            "info" -> {
                val data: JsonNode = try {
                    restClient
                        .get()
                        .uri("${url}/api/map/getBeatMapInfo/${bid}")
                        .headers { it.add("AuthorizationX", token) }
                        .toBody<JsonNode>()
                } catch (e: Exception) {
                    return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON)
                        .body(mapOf("code" to 400, "message" to e.message))
                }

                ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(data)
            }

            "background" -> {
                val data: ByteArray = try {
                    restClient
                        .get()
                        .uri("${url}/api/file/map/bg/${bid}")
                        .headers { it.add("AuthorizationX", token) }
                        .toBody<ByteArray>()
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
