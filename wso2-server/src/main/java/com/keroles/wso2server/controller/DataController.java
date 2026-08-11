package com.keroles.wso2server.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class DataController {
    @GetMapping("/data")
    public Map<String, String> data() {
        return Map.of("message", "hello you are logged in and authorized");
    }
}
