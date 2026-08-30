package com.keroles.wso2server.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@RestController
public class DataController {
    public record User(int id, String fname, String lname, Instant createdAt) {}

    @GetMapping("/data")
    public Map<String, String> data() {
        return Map.of("message", "hello you are logged in and authorized");
    }

    @GetMapping("/not-auth-data")
    public Map<String, Object> publicData() {
        int random = ThreadLocalRandom.current().nextInt(1, 5);
        return Map.of("message", "hello from public data", "random", random);
    }

    @GetMapping("/users")
    public Map<String, Object> users() {
        return Map.of("users",List.of(
                new User(1, "John", "Doe", Instant.parse("2024-01-10T10:00:00Z")),
                new User(2, "Jane", "Smith", Instant.parse("2024-03-22T14:30:00Z")),
                new User(3, "Kero", "Monsef", Instant.parse("2024-06-05T09:15:00Z"))
        ));
    }
}
