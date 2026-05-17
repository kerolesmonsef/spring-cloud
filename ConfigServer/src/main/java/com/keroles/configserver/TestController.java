package com.keroles.configserver;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @Value("${user.dir}")
    private String userDir;

    @GetMapping("/test/userdir")
    public String getUserDir() {
        return "user.dir = " + userDir;
    }
}