package com.keroles.openfeign;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PostResponse {
    private List<Post> posts;
    private int total;
    private int skip;
    private int limit;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Post {
        private int userId;
        private int id;
        private String title;
        private String body;
        private List<String> tags;
        private Map<String, Integer> reactions;
        private int views;
    }
}

