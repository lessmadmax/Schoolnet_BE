package com.cloudproject.community_backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import com.cloudproject.community_backend.entity.Post;
import com.cloudproject.community_backend.entity.PostBoardType;
import com.cloudproject.community_backend.entity.User;
import com.cloudproject.community_backend.repository.PostRepository;
import com.cloudproject.community_backend.repository.UserRepository;
import com.cloudproject.community_backend.service.PostService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Tag(name = "게시물", description = "게시물 관련 API")
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostService postService;

    // DTO 정의 (Swagger에서 Request Body 명확히 보여주기)
    public record PostCreateRequest(
            @Schema(description = "게시물 제목", example = "첫 번째 글")
            String title,

            @Schema(description = "게시물 내용", example = "내용을 입력하세요")
            String content,

            @Schema(description = "게시판 구분", example = "TALK")
            PostBoardType boardType,

            @Schema(description = "선배만 댓글 가능 여부", example = "false")
            Boolean seniorOnlyComment,

            @Schema(description = "모임 게시글 세부 정보")
            MeetingInfo meetingInfo,

            @Schema(description = "질문 게시글 세부 정보")
            QuestionInfo questionInfo
    ) {}

    public record MeetingInfo(
            @Schema(description = "모임 일정 (ISO_LOCAL_DATE_TIME)", example = "2025-11-10T15:00")
            String schedule,

            @Schema(description = "모임 장소", example = "본관 3층 세미나실")
            String location,

            @Schema(description = "모집 인원", example = "10")
            Integer capacity
    ) {}

    public record QuestionInfo(
            @Schema(description = "질문 카테고리", example = "수학")
            String categoryName,

            @Schema(description = "선배 전용 여부", example = "false")
            boolean isForSeniorsOnly
    ) {}

    @Operation(summary = "게시물 작성", description = "새로운 게시물을 작성합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "게시물 작성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @PostMapping
    public Post createPost(@RequestBody PostCreateRequest req) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        String authenticatedEmail = authentication.getName();

        User author = userRepository.findByEmail(authenticatedEmail)
                .orElseThrow(() -> new RuntimeException("작성자 없음"));

        if (req.boardType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "게시판 정보가 필요합니다.");
        }

        Post post = new Post();
        post.setTitle(req.title());
        post.setContent(req.content());
        post.setAuthor(author);
        post.setBoardType(req.boardType());

        return postService.createPost(post, req);
    }

    @Operation(summary = "게시물 전체 조회", description = "등록된 모든 게시물을 조회합니다.")
    @GetMapping
    public List<Post> getAllPosts(@RequestParam(required = false) PostBoardType boardType) {
        if (boardType != null) {
            return postRepository.findByBoardType(boardType);
        }
        return postRepository.findAll();
    }

    @Operation(summary = "게시물 단건 조회", description = "ID로 게시글을 조회합니다.")
    @GetMapping("/{id}")
    public Post getPost(@PathVariable Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));
    }

    @Operation(summary = "게시물 삭제", description = "작성자 본인만 게시물을 삭제할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "게시물 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (작성자만 삭제 가능)"),
            @ApiResponse(responseCode = "404", description = "게시물을 찾을 수 없음")
    })
    @DeleteMapping("/{id}")
    public void deletePost(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        String authenticatedEmail = authentication.getName();

        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));

        // 작성자 확인
        if (!post.getAuthor().getEmail().equals(authenticatedEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "작성자만 삭제할 수 있습니다.");
        }

        postRepository.delete(post);
    }
}
