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
        // 질문 게시판 권한 체크
        Post post = comment.getPost();
        if (post != null && post.getBoardType() == PostBoardType.QUESTION) {
            User author = comment.getAuthor();

            if (author.getIsSeniorVerified() == null || !author.getIsSeniorVerified()) {
                throw new IllegalStateException("질문 게시판은 선배만 답변할 수 있습니다");
            }

            System.out.println(String.format(
                "선배 권한 확인 완료 - 사용자: %s, 학년: %d",
                author.getUsername(), author.getGrade()
            ));
        }

        // 고도화된 AI 필터링 사용
        com.cloudproject.community_backend.dto.FilterResult filterResult =
            contentFilterService.filterContent(
                comment.getContent(),
                "COMMENT",
                comment.getAuthor().getId()
            );

        comment.setBad(filterResult.isBlocked());

        // 차단된 경우 로그 출력
        if (filterResult.isBlocked()) {
            System.out.println(String.format(
                "🚫 댓글 차단됨 - 카테고리: %s, 이유: %s, 신뢰도: %.2f",
                filterResult.getCategory(),
                filterResult.getReason(),
                filterResult.getConfidence()
            ));
        }

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
