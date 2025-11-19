package com.cloudproject.community_backend.repository;

import com.cloudproject.community_backend.entity.Post;
import com.cloudproject.community_backend.entity.PostBoardType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findByBoardType(PostBoardType boardType);
    long countByIsBad(boolean isBad);
}
