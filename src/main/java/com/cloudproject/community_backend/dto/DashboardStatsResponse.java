package com.cloudproject.community_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {

    private FilteringStats filteringStats;
    private ReportStats reportStats;
    private UserStats userStats;
    private PenaltyStats penaltyStats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FilteringStats {
        private long totalPosts;
        private long blockedPosts;
        private double blockRate;
        private long totalComments;
        private long blockedComments;
        private double commentBlockRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReportStats {
        private long totalReports;
        private long pendingReports;
        private long approvedReports;
        private long rejectedReports;
        private double approvalRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserStats {
        private long totalUsers;
        private long studentUsers;
        private long seniorVerifiedUsers;
        private long adminUsers;
        private Map<String, Long> usersBySchool;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PenaltyStats {
        private long totalPenalties;
        private long activePenalties;
        private long warnings;
        private long suspensions;
        private long bans;
    }
}
