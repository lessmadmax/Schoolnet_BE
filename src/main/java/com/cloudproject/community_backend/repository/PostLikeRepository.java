package com.cloudproject.community_backend.repository;

import com.cloudproject.community_backend.entity.Post;
import com.cloudproject.community_backend.entity.PostLike;
import com.cloudproject.community_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    Optional<PostLike> findByPostAndUser(Post post, User user);
    long countByPostAndLiked(Post post, boolean liked);
}
