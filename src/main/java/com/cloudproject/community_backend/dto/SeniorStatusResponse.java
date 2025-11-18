package com.cloudproject.community_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 선배 인증 상태 응답 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeniorStatusResponse {

    private Boolean isSeniorVerified;
    private Integer grade;
    private LocalDateTime verifiedAt;
    private Boolean canAnswerQuestions;  // 질문 게시판 답변 가능 여부
}
