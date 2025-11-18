package com.cloudproject.community_backend.entity;

import lombok.Getter;

/**
 * 신고 처리 상태
 */
@Getter
public enum ReportStatus {
    PENDING("대기중"),
    REVIEWING("검토중"),
    APPROVED("승인됨"),
    REJECTED("반려됨");

    private final String description;

    ReportStatus(String description) {
        this.description = description;
    }
}
