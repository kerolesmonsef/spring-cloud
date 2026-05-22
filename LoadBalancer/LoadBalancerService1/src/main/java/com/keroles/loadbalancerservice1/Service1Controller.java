package com.keroles.loadbalancerservice1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keroles.loadbalancerservice1.dto.PostResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class Service1Controller {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private PostResponse cachedPosts;

    @Value("${server.port}")
    private String port;

    @GetMapping("/info")
    public String info() {
        return "Hello from LoadBalancerService1 running on port " + port;
    }

    @GetMapping("/posts")
    public PostResponse getPosts() throws IOException {
        if (cachedPosts == null) {
            cachedPosts = objectMapper.readValue(
                    new ClassPathResource("posts.json").getInputStream(),
                    PostResponse.class
            );
        }
        return cachedPosts;
    }
}
