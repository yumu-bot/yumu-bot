package com.now.nowbot.controller;


import org.springframework.web.bind.annotation.PathVariable;

//@RestController
//@RequestMapping(produces = "application/json;charset=UTF-8")
public class testController {
//    @RequestMapping("/")
    public String hello(){
        return "ok";
    }

//    @GetMapping("/{path}")
    public String god(@PathVariable("path") String s){
        return "wellcome :"+s;
    }
}
