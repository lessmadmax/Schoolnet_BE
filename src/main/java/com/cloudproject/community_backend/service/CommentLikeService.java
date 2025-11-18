package com.cloudproject.community_backend.service;

import com.cloudproject.community_backend.entity.Comment;
import com.cloudproject.community_backend.entity.CommentLike;
import com.cloudproject.community_backend.entity.User;
import com.cloudproject.community_backend.repository.CommentLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CommentLikeService {

    private final CommentLikeRepository commentLikeRepository;

    public void toggleLike(Comment comment, User user, boolean isLike) {
        Optional<CommentLike> existing = commentLikeRepository.findByCommentAndUser(comment, user);

        if (existing.isPresent()) {
            CommentLike like = existing.get();
            if (like.isLiked() == isLike) {  // getter 수정
                // 같은 버튼 다시 누르면 취소
                commentLikeRepository.delete(like);
            } else {
                // 반대 버튼 눌렀을 경우 변경
                like.setLiked(isLike);      // setter 수정
                commentLikeRepository.save(like);
            }
        } else {
            // 처음 누름
            CommentLike newLike = new CommentLike();
            newLike.setComment(comment);
            newLike.setUser(user);
            newLike.setLiked(isLike);       // setter 수정
            commentLikeRepository.save(newLike);
        }
    }

    public long getLikeCount(Comment comment) {
        return commentLikeRepository.countByCommentAndLiked(comment, true);
    }

    public long getDislikeCount(Comment comment) {
        return commentLikeRepository.countByCommentAndLiked(comment, false);
    }
}
