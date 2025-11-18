package com.cloudproject.community_backend.dto;

import com.cloudproject.community_backend.entity.PenaltyType;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 신고 검토 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRequest {

    @Size(max = 500, message = "검토 의견은 500자 이내로 작성해주세요")
    private String reviewNote;

    /**
     * 제재 유형 (승인 시)
     */
    private PenaltyType penaltyType;

    /**
     * 제재 기간 (일) - 일시정지인 경우
     */
    private Integer duration;
}
