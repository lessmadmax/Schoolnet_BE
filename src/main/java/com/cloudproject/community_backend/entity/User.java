package com.cloudproject.community_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    private String password;

    @Column(nullable = false, unique = true)
    private String email;

    // 학교 정보 (FK)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    @JsonIgnore
    private School school;

    /**
     * 사용자 역할 (학생/관리자)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role = UserRole.STUDENT;

    /**
     * 학년 (1, 2, 3)
     */
    @Column
    private Integer grade;

    /**
     * 선배 인증 여부
     */
    @Column
    private Boolean isSeniorVerified = false;

    /**
     * 선배 인증 시각
     */
    @Column
    private LocalDateTime seniorVerifiedAt;

    private LocalDateTime createdAt = LocalDateTime.now();
}
