package com.cloudproject.community_backend.service;

import com.cloudproject.community_backend.dto.ReportResponse;
import com.cloudproject.community_backend.entity.*;
import com.cloudproject.community_backend.repository.CommentRepository;
import com.cloudproject.community_backend.repository.PostRepository;
import com.cloudproject.community_backend.repository.ReportRepository;
import com.cloudproject.community_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 신고 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ReportService {

    private final ReportRepository reportRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    /**
     * 신고 생성
     */
    public Long createReport(
        TargetType targetType,
        Long targetId,
        ReportReason reason,
        String detail,
        Long reporterId
    ) {
        // 1. 중복 신고 체크
        if (reportRepository.existsByReporterIdAndTargetTypeAndTargetId(
            reporterId, targetType, targetId
        )) {
            throw new IllegalStateException("이미 신고한 콘텐츠입니다");
        }

        // 2. 신고 대상 존재 확인
        validateTargetExists(targetType, targetId);

        // 3. 신고 생성
        User reporter = userRepository.findById(reporterId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        Report report = Report.builder()
            .reporter(reporter)
            .targetType(targetType)
            .targetId(targetId)
            .reason(reason)
            .detail(detail)
            .status(ReportStatus.PENDING)
            .build();

        Report saved = reportRepository.save(report);

        System.out.println(String.format(
            "🚨 신고 접수 - ID: %d, 대상: %s(%d), 사유: %s",
            saved.getId(), targetType, targetId, reason.getDescription()
        ));

        // 4. 신고 누적 시 자동 처리 (5회 이상 시 블라인드)
        handleAutoAction(targetType, targetId);

        return saved.getId();
    }

    /**
     * 신고 대상 존재 확인
     */
    private void validateTargetExists(TargetType targetType, Long targetId) {
        if (targetType == TargetType.POST) {
            if (!postRepository.existsById(targetId)) {
                throw new IllegalArgumentException("게시글을 찾을 수 없습니다");
            }
        } else if (targetType == TargetType.COMMENT) {
            if (!commentRepository.existsById(targetId)) {
                throw new IllegalArgumentException("댓글을 찾을 수 없습니다");
            }
        }
    }

    /**
     * 자동 조치 처리 (5회 이상 신고 시 임시 블라인드)
     */
    private void handleAutoAction(TargetType targetType, Long targetId) {
        long reportCount = reportRepository.countByTargetTypeAndTargetIdAndStatus(
            targetType, targetId, ReportStatus.PENDING
        );

        System.out.println(String.format(
            "📊 신고 누적 개수: %s(%d) - %d건",
            targetType, targetId, reportCount
        ));

        if (reportCount >= 5) {
            System.out.println(String.format(
                "신고 5회 이상 누적 - 자동 블라인드 처리: %s(%d)",
                targetType, targetId
            ));

            if (targetType == TargetType.POST) {
                Post post = postRepository.findById(targetId).orElseThrow();
                post.setBad(true);  // isBad = true로 설정하여 블라인드 처리
                postRepository.save(post);
            } else if (targetType == TargetType.COMMENT) {
                Comment comment = commentRepository.findById(targetId).orElseThrow();
                comment.setBad(true);
                commentRepository.save(comment);
            }

            // TODO: 관리자에게 알림 전송
        }
    }

    /**
     * 내가 한 신고 목록 조회
     */
    @Transactional(readOnly = true)
    public List<ReportResponse> getReportsByReporter(Long reporterId) {
        return reportRepository.findByReporterId(reporterId).stream()
            .map(this::toReportResponse)
            .collect(Collectors.toList());
    }

    /**
     * 중복 신고 체크
     */
    @Transactional(readOnly = true)
    public boolean existsByReporterAndTarget(
        Long reporterId,
        TargetType targetType,
        Long targetId
    ) {
        return reportRepository.existsByReporterIdAndTargetTypeAndTargetId(
            reporterId, targetType, targetId
        );
    }

    /**
     * Report -> ReportResponse 변환
     */
    private ReportResponse toReportResponse(Report report) {
        return ReportResponse.builder()
            .id(report.getId())
            .reporterId(report.getReporter().getId())
            .reporterUsername(report.getReporter().getUsername())
            .targetType(report.getTargetType())
            .targetId(report.getTargetId())
            .reason(report.getReason())
            .detail(report.getDetail())
            .status(report.getStatus())
            .createdAt(report.getCreatedAt())
            .reviewedAt(report.getReviewedAt())
            .reviewNote(report.getReviewNote())
            .build();
    }
}
