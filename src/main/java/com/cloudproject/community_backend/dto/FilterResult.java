package com.cloudproject.community_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 콘텐츠 필터링 결과 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilterResult {

    /**
     * 차단 여부
     */
    private boolean isBlocked;

    /**
     * 카테고리
     * 예: "욕설", "자음욕설", "변형욕설", "따돌림", "은어", "정상" 등
     */
    private String category;

    /**
     * 차단 이유
     */
    private String reason;

    /**
     * 신뢰도 (0.0 ~ 1.0)
     */
    private double confidence;

    /**
     * 감지된 단어 목록
     */
    private List<String> detectedWords;

    /**
     * 차단된 결과 생성 (편의 메서드)
     */
    public static FilterResult blocked(String category, String reason) {
        return FilterResult.builder()
            .isBlocked(true)
            .category(category)
            .reason(reason)
            .confidence(1.0)
            .build();
    }

    /**
     * 정상 결과 생성 (편의 메서드)
     */
    public static FilterResult normal() {
        return FilterResult.builder()
            .isBlocked(false)
            .category("정상")
            .reason("부적절한 내용이 감지되지 않았습니다")
            .confidence(1.0)
            .build();
    }
}
