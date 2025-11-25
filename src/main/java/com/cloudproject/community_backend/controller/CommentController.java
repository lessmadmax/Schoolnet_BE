package com.cloudproject.community_backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import com.cloudproject.community_backend.entity.Comment;
import com.cloudproject.community_backend.entity.Post;
import com.cloudproject.community_backend.entity.User;
import com.cloudproject.community_backend.repository.CommentRepository;
import com.cloudproject.community_backend.repository.PostRepository;
import com.cloudproject.community_backend.repository.UserRepository;
import com.cloudproject.community_backend.service.CommentService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Tag(name = "댓글", description = "댓글 관련 API")
@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    // DTO 정의 (Swagger에서 Request Body를 명확히 보여주기)
    public record CommentCreateRequest(
            @Schema(description = "댓글 내용", example = "좋은 글이네요!")
            String content,

            @Schema(description = "작성자 ID", example = "1")
            Long authorId,

            @Schema(description = "게시물 ID", example = "1")
            Long postId
    
    ) {}

    @Operation(summary = "댓글 작성", description = "새로운 댓글을 작성합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "댓글 작성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (선배 전용)")
    })
    @PostMapping
    public Comment createComment(@RequestBody CommentCreateRequest req) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        String authenticatedEmail = authentication.getName();

        User author = userRepository.findByEmail(authenticatedEmail)
                .orElseThrow(() -> new RuntimeException("작성자 없음"));
        Post post = postRepository.findById(req.postId())
                .orElseThrow(() -> new RuntimeException("게시물 없음"));

        Comment comment = new Comment();
        comment.setContent(req.content());
        comment.setAuthor(author);
        comment.setPost(post);
        comment.setAuthorName(author.getUsername());

        try {
            return commentService.createComment(comment);
        } catch (IllegalStateException e) {
            // 선배 인증 필요 등의 비즈니스 로직 오류를 403으로 반환
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        }
    }

    @Operation(summary = "댓글 조회", description = "특정 게시글의 댓글을 조회합니다. postId 파라미터가 없으면 모든 댓글을 조회합니다.")
    @GetMapping
    public List<Comment> getComments(@RequestParam(required = false) Long postId) {
        if (postId != null) {
            return commentService.getCommentsByPostId(postId);
        }
        return (List<Comment>) commentService.getAllComments();
    }
    @Operation(summary = "악플 댓글 조회", description = "AI가 판별한 악플만 조회합니다.")
    @GetMapping("/bad")
    public List<Comment> getBadComments() {
        return commentService.getBadComments();
    }

    // 리액션 DTO
    public record ReactionRequest(
            @Schema(description = "리액션 타입", example = "helpful")
            String type
    ) {}

    public record ReactionResponse(
            boolean success,
            String message,
            java.util.Map<String, Integer> reactions
    ) {}

    @Operation(summary = "댓글 리액션 추가", description = "댓글에 긍정적 리액션을 추가합니다 (도움됐어요, 친절해요 등)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "리액션 추가 성공"),
            @ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없음")
    })
    @PostMapping("/{id}/reactions")
    public ReactionResponse addReaction(
            @PathVariable Long id,
            @RequestBody ReactionRequest request) {
        // 댓글 존재 여부 확인
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."));

        // 실제로는 DB에 저장해야 하지만, 현재는 로깅만 수행
        System.out.println("댓글 리액션: commentId=" + id + ", type=" + request.type());

        // 임시 응답 (실제 구현 시 DB에서 집계)
        java.util.Map<String, Integer> reactions = new java.util.HashMap<>();
        reactions.put(request.type(), 1);

        return new ReactionResponse(true, "리액션이 추가되었습니다!", reactions);
    }

    @Operation(summary = "댓글 리액션 조회", description = "댓글의 리액션 통계를 조회합니다")
    @GetMapping("/{id}/reactions")
    public java.util.Map<String, Integer> getReactions(@PathVariable Long id) {
        // 임시 응답 (실제 구현 시 DB에서 조회)
        java.util.Map<String, Integer> reactions = new java.util.HashMap<>();
        reactions.put("helpful", 0);
        reactions.put("kind", 0);
        reactions.put("clear", 0);
        return reactions;
    }
}
