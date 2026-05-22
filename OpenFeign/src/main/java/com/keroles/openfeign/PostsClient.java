package com.keroles.openfeign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "posts-client")
public interface PostsClient {
    @GetMapping("/posts")
    PostResponse getPosts();
}

