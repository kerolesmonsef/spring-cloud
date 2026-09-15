package com.keroles.soapserver.post.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "posts")
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String description;

    private Instant createdAt;

    @jakarta.persistence.Column(name = "user_id", nullable = false)
    private Long userId;

    protected Post() {
    }

    public Post(String title, String description, Long userId) {
        this.title = title;
        this.description = description;
        this.userId = userId;
        this.createdAt = Instant.now();
    }
}
