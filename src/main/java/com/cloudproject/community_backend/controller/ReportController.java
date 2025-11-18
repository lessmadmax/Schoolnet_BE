package com.cloudproject.community_backend.controller;

import com.cloudproject.community_backend.dto.ApiResponse;
import com.cloudproject.community_backend.dto.ReportRequest;
import com.cloudproject.community_backend.dto.ReportResponse;
import com.cloudproject.community_backend.entity.TargetType;
import com.cloudproject.community_backend.security.JwtUtil;
import com.cloudproject.community_backend.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 신고 Controller
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Report", description = "신고 API")
public class ReportController {

    private final ReportService reportService;
    private final JwtUtil jwtUtil;

    /**
     * 게시글/댓글 신고
     */
    @PostMapping
    @Operation(summary = "신고 접수", description = "게시글 또는 댓글을 신고합니다")
    public ResponseEntity<ApiResponse<Long>> createReport(
        @RequestBody @Valid ReportRequest request,
        HttpServletRequest httpRequest
    ) {
        Long userId = getUserIdFromToken(httpRequest);

        try {
            Long reportId = reportService.createReport(
                request.getTargetType(),
                request.getTargetId(),
                request.getReason(),
                request.getDetail(),
                userId
            );

            return ResponseEntity.ok(
                ApiResponse.success("신고가 접수되었습니다", reportId)
            );
        } catch (IllegalStateException e) {
            // 중복 신고
            return ResponseEntity.badRequest().body(
                ApiResponse.error(e.getMessage())
            );
        } catch (IllegalArgumentException e) {
            // 대상을 찾을 수 없음
            return ResponseEntity.badRequest().body(
                ApiResponse.error(e.getMessage())
            );
        }
    }

    /**
     * 내가 한 신고 목록 조회
     */
    @GetMapping("/my")
    @Operation(summary = "내 신고 목록", description = "내가 작성한 신고 목록을 조회합니다")
    public ResponseEntity<List<ReportResponse>> getMyReports(
        HttpServletRequest httpRequest
    ) {
        Long userId = getUserIdFromToken(httpRequest);
        List<ReportResponse> reports = reportService.getReportsByReporter(userId);
        return ResponseEntity.ok(reports);
    }

    /**
     * 중복 신고 체크
     */
    @GetMapping("/check")
    @Operation(summary = "중복 신고 확인", description = "이미 신고한 콘텐츠인지 확인합니다")
    public ResponseEntity<Boolean> checkDuplicateReport(
        @RequestParam TargetType targetType,
        @RequestParam Long targetId,
        HttpServletRequest httpRequest
    ) {
        Long userId = getUserIdFromToken(httpRequest);
        boolean exists = reportService.existsByReporterAndTarget(
            userId,
            targetType,
            targetId
        );
        return ResponseEntity.ok(exists);
    }

    /**
     * JWT 토큰에서 사용자 ID 추출
     */
    private Long getUserIdFromToken(HttpServletRequest request) {
        String token = extractTokenFromRequest(request);
        if (token == null) {
            throw new IllegalStateException("인증 토큰이 없습니다");
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
