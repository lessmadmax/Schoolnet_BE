package com.cloudproject.community_backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import com.cloudproject.community_backend.entity.Post;
import com.cloudproject.community_backend.repository.PostRepository;
import com.cloudproject.community_backend.service.GeminiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "AI 봇", description = "AI 답변 생성 관련 API")
@RestController
@RequestMapping("/bot")
@RequiredArgsConstructor
public class BotController {

    private final GeminiService geminiService;
    private final PostRepository postRepository;

    @Operation(summary = "AI 답변 생성", description = "질문에 대한 AI 답변을 생성합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "AI 답변 생성 성공"),
            @ApiResponse(responseCode = "404", description = "질문을 찾을 수 없음")
    })
    @PostMapping("/answer")
    public ResponseEntity<AIAnswerResponse> generateAnswer(@RequestParam Long questionId) {
        // 질문 게시물 조회
        Post post = postRepository.findById(questionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "질문을 찾을 수 없습니다."));

        // 질문 내용 준비
        String questionContent = post.getTitle() + "\n\n" + post.getContent();

        // AI 답변 생성
        String aiAnswer = geminiService.generateAnswer(questionContent);

        return ResponseEntity.ok(new AIAnswerResponse(aiAnswer));
    }

    @Operation(summary = "AI 답변 평가", description = "AI 답변에 대한 피드백을 저장합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "평가 저장 성공")
    })
    @PostMapping("/rate")
    public ResponseEntity<RateResponse> rateAnswer(
            @RequestParam String botAnswerId,
            @RequestParam Boolean isHelpful) {
        // AI 답변 평가 로깅 (실제 저장은 나중에 구현 가능)
        System.out.println("AI 답변 평가: " + botAnswerId + ", 도움됨: " + isHelpful);

        String message = isHelpful ? "피드백 감사합니다!" : "피드백이 전달되었습니다.";
        return ResponseEntity.ok(new RateResponse(true, message));
    }

    // 응답 DTO
    public record AIAnswerResponse(String answer) {}
    public record RateResponse(boolean success, String message) {}
}
