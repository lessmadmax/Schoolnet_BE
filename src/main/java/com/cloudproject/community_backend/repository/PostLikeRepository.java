package com.cloudproject.community_backend.repository;

import com.cloudproject.community_backend.entity.Post;
import com.cloudproject.community_backend.entity.PostLike;
import com.cloudproject.community_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    Optional<PostLike> findByPostAndUser(Post post, User user);

    // ❌ 잘못된 메서드
    // long countByPostAndLike(Post post, boolean like);

    // 올바른 메서드
    long countByPostAndLiked(Post post, boolean liked);
}
