package com.mathstrokes.ranking.dto;

import java.util.List;

public record LeaderboardResponse(
        Long testId,
        String testTitle,
        boolean rankingEnabled,
        int totalCandidates,
        List<LeaderboardEntryResponse> entries) {
}
