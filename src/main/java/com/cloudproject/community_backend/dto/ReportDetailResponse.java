package com.cloudproject.community_backend.dto;

import com.cloudproject.community_backend.entity.ReportReason;
import com.cloudproject.community_backend.entity.ReportStatus;
import com.cloudproject.community_backend.entity.TargetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 신고 상세 응답 DTO (관리자용)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportDetailResponse {

    private Long id;
    private Long reporterId;
    private String reporterUsername;
    private TargetType targetType;
    private Long targetId;
    private String targetContent;  // 신고 대상의 내용
    private Long targetAuthorId;   // 신고 대상 작성자 ID
    private String targetAuthorUsername;  // 신고 대상 작성자 이름
    private ReportReason reason;
    private String detail;
    private ReportStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
    private String reviewNote;
    private Long reviewerId;
    private String reviewerUsername;
}
