package com.cloudproject.community_backend.dto;

import com.cloudproject.community_backend.entity.ReportReason;
import com.cloudproject.community_backend.entity.TargetType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 신고 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReportRequest {

    @NotNull(message = "신고 대상 타입은 필수입니다")
    private TargetType targetType;

    @NotNull(message = "신고 대상 ID는 필수입니다")
    private Long targetId;

    @NotNull(message = "신고 사유는 필수입니다")
    private ReportReason reason;

    @Size(max = 500, message = "상세 설명은 500자 이내로 작성해주세요")
    private String detail;
}
