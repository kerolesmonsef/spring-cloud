package com.keroles.openfeign;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/posts")
public class PostsController {
    private final PostsClient postsClient;

    public PostsController(PostsClient postsClient) {
        this.postsClient = postsClient;
    }

    @GetMapping
    public PostResponse listPosts() {
        return postsClient.getPosts();
    }
}

