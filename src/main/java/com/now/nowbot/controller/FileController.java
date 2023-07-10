package com.now.nowbot.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.file.Files;
import java.nio.file.Path;

//@RestController
//@RequestMapping(value = "/File",produces = "application/json;charset=UTF-8")
public class FileController {
//    @PostMapping(value = "/u/{filename}", produces = "multipart/form-data")
    public String upload(@PathVariable("filename") String name, MultipartHttpServletRequest file){
         file.getFileMap().forEach(this::ref);
        return name + "->";
    }

//    @GetMapping("/d/{filename}")
    public void download(@PathVariable("filename") String name, HttpServletResponse response) throws IOException {
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition","attachment;filename="+name);
        var w = Channels.newChannel(response.getOutputStream());

        w.write(ByteBuffer.wrap(Files.readAllBytes(Path.of(""))));
        w.close();
    }
    private void ref(String name, MultipartFile file){
        System.out.println(name+"'s size is "+file.getSize());
    }
}
