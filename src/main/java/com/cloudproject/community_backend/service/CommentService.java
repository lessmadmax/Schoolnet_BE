package com.cloudproject.community_backend.service;

import com.cloudproject.community_backend.entity.Comment;
import com.cloudproject.community_backend.entity.Post;
import com.cloudproject.community_backend.entity.PostBoardType;
import com.cloudproject.community_backend.entity.User;
import com.cloudproject.community_backend.repository.CommentRepository;
import com.cloudproject.community_backend.repository.PostRepository;
import com.cloudproject.community_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;


@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final ContentFilterService contentFilterService;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public Comment createComment(Comment comment) {
        Post post = comment.getPost();
        User author = comment.getAuthor();

        // 질문 게시판 체크 (기존 로직)
        if (post != null && post.getBoardType() == PostBoardType.QUESTION) {
            if (author.getIsSeniorVerified() == null || !author.getIsSeniorVerified()) {
                throw new IllegalStateException("질문 게시판은 선배만 답변할 수 있습니다");
            }
        }

        // 선배 전용 댓글 체크 (새로운 로직)
        if (post != null && post.isSeniorOnlyComment()) {
            if (author.getIsSeniorVerified() == null || !author.getIsSeniorVerified()) {
                throw new IllegalStateException("이 게시물은 선배만 댓글을 달 수 있습니다");
            }
        }

        com.cloudproject.community_backend.dto.FilterResult filterResult =
            contentFilterService.filterContent(
                comment.getContent(),
                "COMMENT",
                comment.getAuthor().getId()
            );

        comment.setBad(filterResult.isBlocked());

        return commentRepository.save(comment);
    }

    public List<Comment> getBadComments() {
        return commentRepository.findAll()
                .stream()
                .filter(Comment::isBad)
                .toList();
    }
    
    

    public Iterable<Comment> getAllComments() {
        return commentRepository.findAll();
    }
}
