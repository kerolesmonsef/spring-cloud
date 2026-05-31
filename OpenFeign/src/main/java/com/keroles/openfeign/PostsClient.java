package com.keroles.openfeign;

import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "posts-client")
public interface PostsClient {
    @GetMapping("/posts")
    @Retry(name = "posts-client")
    PostResponse getPosts();
}

