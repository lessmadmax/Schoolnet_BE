package com.cloudproject.community_backend.service;

import com.cloudproject.community_backend.entity.Post;
import com.cloudproject.community_backend.entity.PostLike;
import com.cloudproject.community_backend.entity.User;
import com.cloudproject.community_backend.repository.PostLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PostLikeService {

    private final PostLikeRepository postLikeRepository;

    public void toggleLike(Post post, User user, boolean isLike) {
        Optional<PostLike> existing = postLikeRepository.findByPostAndUser(post, user);

        if (existing.isPresent()) {
            PostLike like = existing.get();
            if (like.isLiked() == isLike) {   // getter 변경
                // 같은 버튼 다시 누르면 취소
                postLikeRepository.delete(like);
            } else {
                // 반대 버튼 눌렀을 경우 변경
                like.setLiked(isLike);       // setter 변경
                postLikeRepository.save(like);
            }
        } else {
            // 처음 누름
            PostLike newLike = new PostLike();
            newLike.setPost(post);
            newLike.setUser(user);
            newLike.setLiked(isLike);        // setter 변경
            postLikeRepository.save(newLike);
        }
    }

    public long getLikeCount(Post post) {
        return postLikeRepository.countByPostAndLiked(post, true);
    }
    
    public long getDislikeCount(Post post) {
        return postLikeRepository.countByPostAndLiked(post, false);
    }
    
}
