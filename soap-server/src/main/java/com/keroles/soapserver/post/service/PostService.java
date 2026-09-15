package com.keroles.soapserver.post.service;

import com.keroles.soapserver.post.model.Post;
import com.keroles.soapserver.post.repository.PostRepository;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PostService {
    private final PostRepository postRepository;

    public PostService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    public List<Post> findAll() {
        return postRepository.findAll();
    }

    public Optional<Post> findById(long id) {
        return postRepository.findById(id);
    }

    public Post create(String title, String description, Long userId) {
        return postRepository.save(new Post(title, description, userId));
    }

    public boolean update(long id, String title, String description) {
        return postRepository.findById(id).map(post -> {
            post.setTitle(title);
            post.setDescription(description);
            postRepository.save(post);
            return true;
        }).orElse(false);
    }

    public boolean delete(long id) {
        if (!postRepository.existsById(id)) {
            return false;
        }
        postRepository.deleteById(id);
        return true;
    }
}
