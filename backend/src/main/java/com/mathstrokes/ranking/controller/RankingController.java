package com.mathstrokes.ranking.controller;

import com.mathstrokes.ranking.dto.LeaderboardResponse;
import com.mathstrokes.ranking.service.RankingService;
import com.mathstrokes.security.service.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rankings")
@Tag(name = "Rankings", description = "Leaderboards for comparable tests")
public class RankingController {

    private final RankingService rankingService;

    public RankingController(RankingService rankingService) {
        this.rankingService = rankingService;
    }

    @GetMapping("/tests/{testId}")
    @Operation(summary = "Leaderboard for a test",
            description = "Ranks each student's best evaluated attempt. Empty for tests that draw "
                    + "a different paper per attempt, since those are not comparable.")
    public LeaderboardResponse leaderboard(@PathVariable Long testId) {
        return rankingService.leaderboard(testId,
                SecurityUtils.currentPrincipal().map(p -> p.id()).orElse(null));
    }
}
