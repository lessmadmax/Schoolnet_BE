package com.cloudproject.community_backend.entity;

import lombok.Getter;

/**
 * 제재 상태
 */
@Getter
public enum PenaltyStatus {
    ACTIVE("활성"),
    EXPIRED("만료");

    private final String description;

    PenaltyStatus(String description) {
        this.description = description;
    }
}
