package com.cloudproject.community_backend.controller;

import com.cloudproject.community_backend.dto.ApiResponse;
import com.cloudproject.community_backend.dto.FilterResult;
import com.cloudproject.community_backend.service.ContentFilterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 콘텐츠 필터링 API 컨트롤러
 */
@RestController
@RequestMapping("/api/filter")
@RequiredArgsConstructor
@Tag(name = "Filter", description = "콘텐츠 필터링 API")
public class FilterController {

    private final ContentFilterService contentFilterService;

    /**
     * 실시간 필터링 체크 (간단)
     * GET 방식으로 빠른 체크
     */
    @GetMapping("/check-realtime")
    @Operation(summary = "실시간 필터링 체크", description = "입력 중 실시간으로 욕설을 체크합니다 (기본 욕설 사전만)")
    public ResponseEntity<Map<String, Object>> checkRealtime(@RequestParam String text) {
        try {
            FilterResult result = contentFilterService.filterContent(text, "REALTIME", null);

            return ResponseEntity.ok(Map.of(
                "isHarmful", result.isBlocked(),
                "message", result.isBlocked() ? "부적절한 표현이 감지되었습니다" : "정상",
                "category", result.getCategory()
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "isHarmful", false,
                "message", "체크 실패",
                "category", "ERROR"
            ));
        }
    }

    /**
     * 최종 필터링 체크 (상세)
     * POST 방식으로 상세 분석
     */
    @PostMapping("/check")
    @Operation(summary = "최종 필터링 체크", description = "답변/게시글 작성 전 최종 욕설 체크")
    public ResponseEntity<Map<String, Object>> checkContent(@RequestBody Map<String, String> request) {
        try {
            String text = request.get("text");
            String source = request.getOrDefault("source", "UNKNOWN");

            if (text == null || text.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "isHarmful", false,
                    "message", "텍스트가 비어있습니다",
                    "category", "EMPTY"
                ));
            }

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long userId = null;
            if (authentication != null && authentication.isAuthenticated()) {
                try {
                    userId = Long.parseLong(authentication.getName());
                } catch (NumberFormatException e) {
                    // 이메일인 경우 무시
                }
            }

            FilterResult result = contentFilterService.filterContent(text, source, userId);

            return ResponseEntity.ok(Map.of(
                "isHarmful", result.isBlocked(),
                "message", result.isBlocked()
                    ? result.getReason()
                    : "부적절한 내용이 감지되지 않았습니다",
                "category", result.getCategory(),
                "confidence", result.getConfidence(),
                "detectedWords", result.getDetectedWords() != null ? result.getDetectedWords() : java.util.List.of()
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(Map.of(
                "isHarmful", false,
                "message", "필터링 체크 중 오류가 발생했습니다",
                "category", "ERROR"
            ));
        }
    }
}
