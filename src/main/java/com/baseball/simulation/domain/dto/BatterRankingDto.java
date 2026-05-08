package com.baseball.simulation.domain.dto;

/**
 * 타자 순위 출력용 DTO입니다.
 * 파생 지표(타율·출루율·장타율·OPS)는 BatterStatSnapshot 계산값을 받아 저장합니다.
 */
public record BatterRankingDto(
        String playerName,
        String teamName,
        int    plateAppearances,
        int    atBats,
        int    hits,
        int    doubles,
        int    triples,
        int    homeRuns,
        int    runs,
        int    rbi,
        int    strikeouts,
        int    walks,
        double battingAvg,
        double onBasePct,
        double sluggingPct,
        double ops
) {

    /** [순위] 선수명(팀명) | 지정 지표 포맷의 한 줄 출력 */
    public String toDisplayLine(int rank) {
        return String.format(
                "[%3d] %-8s (%-6s) | %3d타석 %3d타수 %3d안타 %2d홈런 %2d삼진 %2d볼넷"
                        + " | 타율 %.3f  출루율 %.3f  장타율 %.3f  OPS %.3f",
                rank, playerName, teamName,
                plateAppearances, atBats, hits, homeRuns, strikeouts, walks,
                battingAvg, onBasePct, sluggingPct, ops
        );
    }
}
