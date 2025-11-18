package com.cloudproject.community_backend.repository;

import com.cloudproject.community_backend.entity.Report;
import com.cloudproject.community_backend.entity.ReportStatus;
import com.cloudproject.community_backend.entity.TargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 신고 Repository
 */
@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    /**
     * 신고자 + 대상으로 중복 신고 체크
     */
    boolean existsByReporterIdAndTargetTypeAndTargetId(
        Long reporterId,
        TargetType targetType,
        Long targetId
    );

    /**
     * 신고자가 작성한 신고 목록
     */
    List<Report> findByReporterId(Long reporterId);

    /**
     * 특정 대상에 대한 신고 개수 (상태별)
     */
    long countByTargetTypeAndTargetIdAndStatus(
        TargetType targetType,
        Long targetId,
        ReportStatus status
    );

    /**
     * 특정 상태의 신고 개수
     */
    long countByStatus(ReportStatus status);

    /**
     * 상태별 신고 목록 조회 (페이징)
     */
    Page<Report> findByStatus(ReportStatus status, Pageable pageable);

    /**
     * 모든 신고 목록 (페이징)
     */
    Page<Report> findAll(Pageable pageable);
}
