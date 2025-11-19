package com.cloudproject.community_backend.controller;

import com.cloudproject.community_backend.dto.ApiResponse;
import com.cloudproject.community_backend.dto.DashboardStatsResponse;
import com.cloudproject.community_backend.dto.ReportDetailResponse;
import com.cloudproject.community_backend.dto.ReviewRequest;
import com.cloudproject.community_backend.entity.ReportStatus;
import com.cloudproject.community_backend.security.JwtUtil;
import com.cloudproject.community_backend.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 관리자 Controller
 *
 * 주의: 실제 운영 환경에서는 @PreAuthorize("hasRole('ADMIN')") 등을 사용하여
 * 관리자 권한 체크를 반드시 추가해야 합니다.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "관리자 API (관리자만 접근 가능)")
public class AdminController {

    private final AdminService adminService;
    private final JwtUtil jwtUtil;

    /**
     * 신고 목록 조회 (필터링, 페이징)
     */
    @GetMapping("/reports")
    @Operation(summary = "신고 목록 조회", description = "관리자가 신고 목록을 조회합니다 (페이징)")
    public ResponseEntity<Page<ReportDetailResponse>> getReports(
        @RequestParam(required = false) ReportStatus status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        HttpServletRequest httpRequest
    ) {
        // TODO: 관리자 권한 체크
        verifyAdmin(httpRequest);

        PageRequest pageRequest = PageRequest.of(
            page,
            size,
            Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<ReportDetailResponse> reports = adminService.getReports(status, pageRequest);
        return ResponseEntity.ok(reports);
    }

    /**
     * 신고 상세 조회
     */
    @GetMapping("/reports/{reportId}")
    @Operation(summary = "신고 상세 조회", description = "특정 신고의 상세 정보를 조회합니다")
    public ResponseEntity<ReportDetailResponse> getReportDetail(
        @PathVariable Long reportId,
        HttpServletRequest httpRequest
    ) {
        verifyAdmin(httpRequest);

        ReportDetailResponse detail = adminService.getReportDetail(reportId);
        return ResponseEntity.ok(detail);
    }

    /**
     * 신고 승인 (게시글/댓글 삭제)
     */
    @PutMapping("/reports/{reportId}/approve")
    @Operation(summary = "신고 승인", description = "신고를 승인하고 콘텐츠를 삭제합니다")
    public ResponseEntity<ApiResponse<Void>> approveReport(
        @PathVariable Long reportId,
        @RequestBody @Valid ReviewRequest request,
        HttpServletRequest httpRequest
    ) {
        Long adminId = getUserIdFromToken(httpRequest);
        verifyAdmin(httpRequest);

        try {
            adminService.approveReport(
                reportId,
                request.getReviewNote(),
                request.getPenaltyType(),
                request.getDuration(),
                adminId
            );

            return ResponseEntity.ok(
                ApiResponse.success("신고가 승인되었습니다")
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(e.getMessage())
            );
        }
    }

    /**
     * 신고 반려
     */
    @PutMapping("/reports/{reportId}/reject")
    @Operation(summary = "신고 반려", description = "신고를 반려합니다")
    public ResponseEntity<ApiResponse<Void>> rejectReport(
        @PathVariable Long reportId,
        @RequestBody @Valid ReviewRequest request,
        HttpServletRequest httpRequest
    ) {
        Long adminId = getUserIdFromToken(httpRequest);
        verifyAdmin(httpRequest);

        try {
            adminService.rejectReport(
                reportId,
                request.getReviewNote(),
                adminId
            );

            return ResponseEntity.ok(
                ApiResponse.success("신고가 반려되었습니다")
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(e.getMessage())
            );
        }
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

    @GetMapping("/stats")
    @Operation(summary = "대시보드 통계 조회", description = "관리자 대시보드에 표시할 각종 통계 정보를 조회합니다")
    public ResponseEntity<DashboardStatsResponse> getDashboardStats(HttpServletRequest request) {
        verifyAdmin(request);
        DashboardStatsResponse stats = adminService.getDashboardStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * 관리자 권한 확인
     *
     * TODO: 실제로는 User 엔티티에서 role을 확인하여 ADMIN 권한을 체크해야 합니다.
     * 현재는 임시로 토큰만 확인합니다.
     */
    private void verifyAdmin(HttpServletRequest request) {
        Long userId = getUserIdFromToken(request);
        // TODO: userRepository에서 userId로 User를 조회하고 role이 ADMIN인지 확인
        // if (user.getRole() != UserRole.ADMIN) {
        //     throw new IllegalStateException("관리자 권한이 필요합니다");
        // }
    }
}
