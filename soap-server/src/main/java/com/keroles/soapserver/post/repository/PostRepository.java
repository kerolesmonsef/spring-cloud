package com.keroles.soapserver.post.repository;

import com.keroles.soapserver.post.model.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {
}
