package com.now.nowbot.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.now.nowbot.config.NowbotConfig;
import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping(value = "/pub", method = RequestMethod.GET)
@CrossOrigin("http://localhost:5173")
public class FileController {
    private static final String API_URL = NowbotConfig.BS_API_URL;
    @Resource
    WebClient webClient;
    final Optional<String> token = NowbotConfig.BS_TOKEN;

    /**
     * @throws IOException
     */
    @GetMapping("/api/{type}/{bid}")
    public ResponseEntity<?> download(@PathVariable("type") String type, @PathVariable("bid") Long bid) {
        if ("info".equals(type)) {
            JsonNode data;
            try {
                data = webClient.get().uri(STR."\{API_URL}/api/map/getBeatMapInfo/\{bid}")
                        .headers(h -> token.ifPresent(t -> h.addIfAbsent("AuthorizationX", t)))
                        .retrieve()
                        .bodyToMono(JsonNode.class)
                        .block();
            } catch (Exception e) {
                return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(Map.of("code", 400, "message", e.getMessage()));

            }
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(data);
        }
        if ("background".equals(type)) {
            byte[] data;
            try {
                data = webClient.get().uri(STR."\{API_URL}/api/file/map/bg/\{bid}")
                        .headers(h -> token.ifPresent(t -> h.addIfAbsent("AuthorizationX", t)))
                        .retrieve()
                        .bodyToMono(byte[].class).block();
            } catch (Exception e) {
                return ResponseEntity.status(400).contentType(MediaType.APPLICATION_JSON).body(Map.of("code", 400, "message", e.getMessage()));
            }


            return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(data);
        }
        return ResponseEntity.status(400).build();
    }


    private void ref(String name, MultipartFile file) {
        System.out.println(STR."\{name}'s size is \{file.getSize()}");
    }
}
