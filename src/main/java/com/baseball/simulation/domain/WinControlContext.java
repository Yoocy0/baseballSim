package com.baseball.simulation.domain;

/**
 * 승/패 제어 모드 컨텍스트입니다.
 * <p>
 * winnerTeamId가 null이면 일반(랜덤) 모드입니다.
 * 파사드에서 생성하여 GameLogicService → InningProcessor 순으로 전달됩니다.
 *
 * @param winnerTeamId 반드시 승리해야 하는 팀 ID (null = 일반 모드)
 * @param teamAId      경기 A팀 ID
 * @param teamBId      경기 B팀 ID
 */
public record WinControlContext(
        Long winnerTeamId,
        Long teamAId,
        Long teamBId
) {
    /** 일반(랜덤) 모드 컨텍스트 */
    public static WinControlContext normal() {
        return new WinControlContext(null, null, null);
    }

    /** 승/패 제어 모드인지 여부 */
    public boolean isControlMode() {
        return winnerTeamId != null;
    }

    /**
     * 현재 반이닝이 승리 지정 팀의 공격 이닝인지 확인합니다.
     * isTop=true(초) → A팀 공격 / isTop=false(말) → B팀 공격
     */
    public boolean isWinnerBatting(boolean isTop) {
        if (winnerTeamId == null) return false;
        return (winnerTeamId.equals(teamAId) && isTop)
                || (winnerTeamId.equals(teamBId) && !isTop);
    }

    /**
     * 현재 누적 점수 기준으로 승리 지정 팀이 지고 있는지 확인합니다.
     * (동점 포함 이하 → false, 뒤처지는 경우만 true)
     *
     * @param liveScoreA 이닝 중 실시간 A팀 점수
     * @param liveScoreB 이닝 중 실시간 B팀 점수
     */
    public boolean isWinnerLosing(int liveScoreA, int liveScoreB) {
        if (winnerTeamId == null) return false;
        if (winnerTeamId.equals(teamAId)) return liveScoreA < liveScoreB;
        if (winnerTeamId.equals(teamBId)) return liveScoreB < liveScoreA;
        return false;
    }
}
