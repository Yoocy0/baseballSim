package com.baseball.simulation.domain.dto;

/**
 * 투수 순위 출력용 DTO입니다.
 * 파생 지표(ERA·피안타율·WHIP)는 PitcherStatSnapshot 계산값을 받아 저장합니다.
 */
public record PitcherRankingDto(
        String playerName,
        String teamName,
        int    games,
        int    wins,
        int    losses,
        int    strikeouts,
        int    walks,
        int    inningsX3,
        int    hitsAllowed,
        int    homeRunsAllowed,
        int    battersFaced,
        int    pitchesThrown,
        int    runsAllowed,
        double era,
        double avgAllowed,
        double whip
) {

    /** 출력용 이닝 문자열. 예) 20 → "6.2" */
    public String inningsDisplay() {
        return String.format("%d.%d", inningsX3 / 3, inningsX3 % 3);
    }

    /** [순위] 이름(팀명) | ERA | 승 | 패 | 이닝 | 탈삼진 | 볼넷 | 피안타 | 실점 | 투구수 | WHIP 포맷 */
    public String toDisplayLine(int rank) {
        return String.format(
                "[%3d] %-8s (%-6s) | ERA %5.2f | %2d승 %2d패 | %5s이닝"
                        + " | %3d삼진 | %3d볼넷 | %3d피안 | %3d실점 | %4d투구 | WHIP %.2f",
                rank, playerName, teamName,
                era, wins, losses, inningsDisplay(),
                strikeouts, walks, hitsAllowed, runsAllowed, pitchesThrown, whip
        );
    }
}
