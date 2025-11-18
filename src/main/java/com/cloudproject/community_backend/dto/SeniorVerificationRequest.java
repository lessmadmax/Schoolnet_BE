package com.cloudproject.community_backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 선배 인증 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SeniorVerificationRequest {

    @NotNull(message = "학년은 필수입니다")
    @Min(value = 2, message = "선배 인증은 2학년 이상만 가능합니다")
    @Max(value = 3, message = "학년은 1~3 사이여야 합니다")
    private Integer grade;

    /**
     * 선배 인증 자료 URL (선택)
     */
    private String proofImageUrl;
}
