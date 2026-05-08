package com.baseball.simulation.domain.dto;

/**
 * 박스스코어 - 투수 1행 데이터
 */
public record PitcherBoxLineDto(
        String playerName,
        String teamName,
        int inningsX3,          // 이닝수 × 3 (e.g. 9이닝=27, 6.2이닝=20)
        int pitchesThrown,
        int hitsAllowed,
        int homeRunsAllowed,    // 피홈런
        int strikeouts,
        int walks,              // 볼넷
        int runsAllowed
) {
    public String inningsDisplay() {
        return inningsX3 / 3 + "." + inningsX3 % 3;
    }
}
