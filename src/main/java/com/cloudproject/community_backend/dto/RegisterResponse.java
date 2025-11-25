package com.cloudproject.community_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class RegisterResponse {
    private Long userId;
    private String name;
    private String email;
    private String token;
    private Boolean isSeniorVerified;
}
