package com.cloudproject.community_backend.controller;

import com.cloudproject.community_backend.entity.Comment;
import com.cloudproject.community_backend.entity.User;
import com.cloudproject.community_backend.repository.CommentRepository;
import com.cloudproject.community_backend.repository.UserRepository;
import com.cloudproject.community_backend.security.JwtUtil;
import com.cloudproject.community_backend.service.CommentLikeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
@Tag(name = "댓글 좋아요/싫어요", description = "댓글 좋아요/싫어요 관련 API")
public class CommentLikeController {

    private final CommentLikeService commentLikeService;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @PostMapping("/{id}/like")
    @Operation(summary = "댓글 좋아요", description = "특정 댓글에 대해 좋아요를 누릅니다. JWT 토큰으로 사용자 인증")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "댓글 좋아요 처리 완료",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{ \"message\": \"댓글 좋아요 처리 완료\", \"likes\": 5, \"dislikes\": 2 }"
                )
            )
        ),
        @ApiResponse(responseCode = "404", description = "댓글 또는 유저를 찾을 수 없음"),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    public ResponseEntity<Map<String, Object>> likeComment(
            @Parameter(description = "댓글 ID", example = "1") @PathVariable Long id,
            HttpServletRequest request) {

        Long userId = getUserIdFromToken(request);

        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"));

        commentLikeService.toggleLike(comment, user, true);

        long likeCount = commentLikeService.getLikeCount(comment);
        long dislikeCount = commentLikeService.getDislikeCount(comment);

        return ResponseEntity.ok(Map.of(
                "message", "댓글 좋아요 처리 완료",
                "likes", likeCount,
                "dislikes", dislikeCount
        ));
    }

    @PostMapping("/{id}/dislike")
    @Operation(summary = "댓글 싫어요", description = "특정 댓글에 대해 싫어요를 누릅니다. JWT 토큰으로 사용자 인증")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "댓글 싫어요 처리 완료",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{ \"message\": \"댓글 싫어요 처리 완료\", \"likes\": 3, \"dislikes\": 4 }"
                )
            )
        ),
        @ApiResponse(responseCode = "404", description = "댓글 또는 유저를 찾을 수 없음"),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    public ResponseEntity<Map<String, Object>> dislikeComment(
            @Parameter(description = "댓글 ID", example = "1") @PathVariable Long id,
            HttpServletRequest request) {

        Long userId = getUserIdFromToken(request);

        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"));

        commentLikeService.toggleLike(comment, user, false);

        long likeCount = commentLikeService.getLikeCount(comment);
        long dislikeCount = commentLikeService.getDislikeCount(comment);

        return ResponseEntity.ok(Map.of(
                "message", "댓글 싫어요 처리 완료",
                "likes", likeCount,
                "dislikes", dislikeCount
        ));
    }

    @GetMapping("/{id}/likes")
    @Operation(summary = "댓글 좋아요/싫어요 수 조회", description = "특정 댓글의 좋아요/싫어요 개수를 반환합니다.")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = "{ \"likes\": 7, \"dislikes\": 1 }")
            )
        ),
        @ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없음")
    })
    public ResponseEntity<Map<String, Long>> getCommentLikes(
            @Parameter(description = "댓글 ID", example = "1") @PathVariable Long id) {

        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다"));
        long likeCount = commentLikeService.getLikeCount(comment);
        long dislikeCount = commentLikeService.getDislikeCount(comment);
        return ResponseEntity.ok(Map.of("likes", likeCount, "dislikes", dislikeCount));
    }

    /**
     * JWT 토큰에서 사용자 ID 추출
     */
    private Long getUserIdFromToken(HttpServletRequest request) {
        String token = extractTokenFromRequest(request);
        if (token == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증 토큰이 없습니다");
        }
        return jwtUtil.getUserIdFromToken(token);
    }

    /**
     * 요청에서 JWT 토큰 추출
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
