package com.cloudproject.community_backend.entity;

import lombok.Getter;

/**
 * 사용자 역할
 */
@Getter
public enum UserRole {
    STUDENT("학생"),
    ADMIN("관리자");

    private final String description;

    UserRole(String description) {
        this.description = description;
    }
}
