package com.cloudproject.community_backend.entity;

import lombok.Getter;

/**
 * 제재 유형
 */
@Getter
public enum PenaltyType {
    WARNING("경고"),
    SUSPENSION("일시정지"),
    BAN("영구정지");

    private final String description;

    PenaltyType(String description) {
        this.description = description;
    }
}
