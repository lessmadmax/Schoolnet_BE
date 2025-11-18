package com.cloudproject.community_backend.service;

import com.cloudproject.community_backend.dto.ReportDetailResponse;
import com.cloudproject.community_backend.entity.*;
import com.cloudproject.community_backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 관리자 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AdminService {

    private final ReportRepository reportRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final UserPenaltyRepository userPenaltyRepository;

    /**
     * 신고 목록 조회 (페이징)
     */
    @Transactional(readOnly = true)
    public Page<ReportDetailResponse> getReports(
        ReportStatus status,
        Pageable pageable
    ) {
        Page<Report> reports;
        if (status != null) {
            reports = reportRepository.findByStatus(status, pageable);
        } else {
            reports = reportRepository.findAll(pageable);
        }

        return reports.map(this::toReportDetailResponse);
    }

    /**
     * 신고 상세 조회
     */
    @Transactional(readOnly = true)
    public ReportDetailResponse getReportDetail(Long reportId) {
        Report report = reportRepository.findById(reportId)
            .orElseThrow(() -> new IllegalArgumentException("신고를 찾을 수 없습니다"));
        return toReportDetailResponse(report);
    }

    /**
     * 신고 승인 (콘텐츠 삭제 + 제재 옵션)
     */
    public void approveReport(
        Long reportId,
        String reviewNote,
        PenaltyType penaltyType,
        Integer duration,
        Long adminId
    ) {
        Report report = reportRepository.findById(reportId)
            .orElseThrow(() -> new IllegalArgumentException("신고를 찾을 수 없습니다"));

        User admin = userRepository.findById(adminId)
            .orElseThrow(() -> new IllegalArgumentException("관리자를 찾을 수 없습니다"));

        // 1. 신고 상태 변경
        report.setStatus(ReportStatus.APPROVED);
        report.setReviewNote(reviewNote);
        report.setReviewer(admin);
        report.setReviewedAt(LocalDateTime.now());

        // 2. 신고 대상 삭제 (isBad = true)
        Long authorId = markContentAsBad(report.getTargetType(), report.getTargetId());

        System.out.println(String.format(
            "신고 승인 - ID: %d, 대상: %s(%d), 검토자: %s",
            reportId, report.getTargetType(), report.getTargetId(), admin.getUsername()
        ));

        // 3. 작성자 제재 (옵션)
        if (penaltyType != null && authorId != null) {
            penalizeUser(authorId, penaltyType, duration, reviewNote, adminId);
        }
    }

    /**
     * 신고 반려
     */
    public void rejectReport(
        Long reportId,
        String reviewNote,
        Long adminId
    ) {
        Report report = reportRepository.findById(reportId)
            .orElseThrow(() -> new IllegalArgumentException("신고를 찾을 수 없습니다"));

        User admin = userRepository.findById(adminId)
            .orElseThrow(() -> new IllegalArgumentException("관리자를 찾을 수 없습니다"));

        report.setStatus(ReportStatus.REJECTED);
        report.setReviewNote(reviewNote);
        report.setReviewer(admin);
        report.setReviewedAt(LocalDateTime.now());

        System.out.println(String.format(
            "❌ 신고 반려 - ID: %d, 검토자: %s",
            reportId, admin.getUsername()
        ));
    }

    /**
     * 콘텐츠를 부적절로 표시 (isBad = true)
     * @return 작성자 ID
     */
    private Long markContentAsBad(TargetType targetType, Long targetId) {
        if (targetType == TargetType.POST) {
            Post post = postRepository.findById(targetId).orElseThrow();
            post.setBad(true);
            postRepository.save(post);
            return post.getAuthor().getId();
        } else if (targetType == TargetType.COMMENT) {
            Comment comment = commentRepository.findById(targetId).orElseThrow();
            comment.setBad(true);
            commentRepository.save(comment);
            return comment.getAuthor().getId();
        }
        return null;
    }

    /**
     * 사용자 제재
     */
    public void penalizeUser(
        Long userId,
        PenaltyType penaltyType,
        Integer duration,
        String reason,
        Long adminId
    ) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        User admin = userRepository.findById(adminId)
            .orElseThrow(() -> new IllegalArgumentException("관리자를 찾을 수 없습니다"));

        LocalDateTime startDate = LocalDateTime.now();
        LocalDateTime endDate = null;

        if (penaltyType == PenaltyType.SUSPENSION && duration != null) {
            endDate = startDate.plusDays(duration);
        }

        UserPenalty penalty = UserPenalty.builder()
            .user(user)
            .type(penaltyType)
            .reason(reason)
            .startDate(startDate)
            .endDate(endDate)
            .status(PenaltyStatus.ACTIVE)
            .admin(admin)
            .build();

        userPenaltyRepository.save(penalty);

        System.out.println(String.format(
            "⚖️ 사용자 제재 - 대상: %s, 유형: %s, 기간: %s일, 사유: %s",
            user.getUsername(),
            penaltyType.getDescription(),
            duration != null ? duration : "무제한",
            reason
        ));
    }

    /**
     * Report -> ReportDetailResponse 변환
     */
    private ReportDetailResponse toReportDetailResponse(Report report) {
        // 신고 대상 콘텐츠 정보 조회
        String targetContent = "";
        Long targetAuthorId = null;
        String targetAuthorUsername = "";

        if (report.getTargetType() == TargetType.POST) {
            postRepository.findById(report.getTargetId()).ifPresent(post -> {
            });
        } else if (report.getTargetType() == TargetType.COMMENT) {
            commentRepository.findById(report.getTargetId()).ifPresent(comment -> {
            });
        }

        return ReportDetailResponse.builder()
            .id(report.getId())
            .reporterId(report.getReporter().getId())
            .reporterUsername(report.getReporter().getUsername())
            .targetType(report.getTargetType())
            .targetId(report.getTargetId())
            .targetContent(targetContent)
            .targetAuthorId(targetAuthorId)
            .targetAuthorUsername(targetAuthorUsername)
            .reason(report.getReason())
            .detail(report.getDetail())
            .status(report.getStatus())
            .createdAt(report.getCreatedAt())
            .reviewedAt(report.getReviewedAt())
            .reviewNote(report.getReviewNote())
            .reviewerId(report.getReviewer() != null ? report.getReviewer().getId() : null)
            .reviewerUsername(report.getReviewer() != null ? report.getReviewer().getUsername() : null)
            .build();
    }
}
