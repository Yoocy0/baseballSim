package com.baseball.simulation.domain;

public record GameScheduleItem(
        Long gameId,
        String homeTeamName,
        int homeScore,
        String awayTeamName,
        int awayScore
) {
}

