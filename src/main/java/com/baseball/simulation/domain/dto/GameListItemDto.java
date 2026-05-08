package com.baseball.simulation.domain.dto;

/**
 * 경기 목록 출력용 DTO (날짜, 팀명, 스코어 포함)
 */
public record GameListItemDto(
        Long gameId,
        String gameDate,    // "2026-05-08" 형식
        String teamAName,
        String teamBName,
        int scoreA,
        int scoreB,
        String status
) {
    public String toDisplayLine(int index) {
        return String.format("[%2d] %s | %-8s %2d : %-2d %-8s",
                index, gameDate, teamAName, scoreA, scoreB, teamBName);
    }
}
