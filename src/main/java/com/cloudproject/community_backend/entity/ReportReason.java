package com.cloudproject.community_backend.entity;

import lombok.Getter;

/**
 * 신고 사유
 */
@Getter
public enum ReportReason {
    PROFANITY("욕설"),
    SLANDER("비방"),
    BULLYING("따돌림"),
    SPAM("스팸"),
    INAPPROPRIATE("부적절한 콘텐츠"),
    PRIVACY("개인정보 노출"),
    ADVERTISEMENT("광고"),
    ETC("기타");

    private final String description;

    ReportReason(String description) {
        this.description = description;
    }
}
