package com.cloudproject.community_backend.repository;

import com.cloudproject.community_backend.entity.Comment;
import com.cloudproject.community_backend.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    long countByIsBad(boolean isBad);
    List<Comment> findByPost(Post post);
    List<Comment> findByPostId(Long postId);
}
