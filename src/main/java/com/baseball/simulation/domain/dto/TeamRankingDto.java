package com.baseball.simulation.domain.dto;

/**
 * 팀 순위 출력용 DTO입니다.
 * winRate는 엔티티 필드가 아닌 TeamRecord.winRate() 계산값을 받아 저장합니다.
 */
public record TeamRankingDto(
        String teamName,
        int wins,
        int losses,
        double winRate
) {

    /** [순위] 팀명 | 10승 | 2패 | 0.833 형식의 콘솔 출력 문자열 */
    public String toDisplayLine(int rank) {
        return String.format("[%d위] %-8s | %2d승 | %2d패 | %.3f",
                rank, teamName, wins, losses, winRate);
    }
}
