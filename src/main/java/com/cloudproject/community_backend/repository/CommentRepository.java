package com.cloudproject.community_backend.repository;

import com.cloudproject.community_backend.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    long countByIsBad(boolean isBad);
}
