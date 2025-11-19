package com.cloudproject.community_backend.repository;

import com.cloudproject.community_backend.entity.PenaltyStatus;
import com.cloudproject.community_backend.entity.PenaltyType;
import com.cloudproject.community_backend.entity.UserPenalty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 사용자 제재 Repository
 */
@Repository
public interface UserPenaltyRepository extends JpaRepository<UserPenalty, Long> {

    /**
     * 사용자의 활성 제재 목록
     */
    List<UserPenalty> findByUserIdAndStatus(Long userId, PenaltyStatus status);

    /**
     * 사용자의 모든 제재 이력
     */
    List<UserPenalty> findByUserId(Long userId);

    long countByStatus(PenaltyStatus status);
    long countByType(PenaltyType type);
}
