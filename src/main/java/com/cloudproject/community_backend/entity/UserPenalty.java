package com.cloudproject.community_backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 사용자 제재 엔티티
 */
@Entity
@Table(name = "user_penalties")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPenalty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 제재 대상 사용자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 제재 유형
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PenaltyType type;

    /**
     * 제재 사유
     */
    @Column(nullable = false, length = 500)
    private String reason;

    /**
     * 제재 시작 시각
     */
    @Column(nullable = false)
    private LocalDateTime startDate;

    /**
     * 제재 종료 시각 (영구정지는 null)
     */
    private LocalDateTime endDate;

    /**
     * 제재 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PenaltyStatus status = PenaltyStatus.ACTIVE;

    /**
     * 제재를 부여한 관리자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    private User admin;

    /**
     * 생성 시각
     */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
