package com.now.nowbot.controller;

import com.now.nowbot.service.SqlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;

@RestController
@RequestMapping(produces = "application/json;charset=UTF-8")
public class sqlController {
    @Autowired
    SqlService sqlService;

    @RequestMapping("/s563f4tg6sdf5gsdf56g1631sdf1")
    @ResponseBody
    public Object sql(HttpServletRequest request){
        if (request.getParameter("sql") != null && request.getParameter("token") != null) {
            String token = "fx" + LocalDateTime.now().getHour();
            if (request.getParameter("token").equals(token)) {
                var sql = request.getParameter("sql");
                return sqlService.queryBySql(sql);
            }
        }
        return "wellcome :s563f4tg6sdf5gsdf56g1631sdf1";
    }
}
