package com.baseball.simulation.domain;

/**
 * 스코어 모드 컨텍스트입니다.
 * <p>
 * 양 팀의 최종 타겟 점수를 보관하며, GameLogicService → InningProcessor 순으로 전달됩니다.
 * targetScoreA / targetScoreB 가 음수이면 일반 모드(스코어 제어 없음)를 의미합니다.
 *
 * @param targetScoreA A팀(away)의 최종 타겟 점수 (-1 = 비활성)
 * @param targetScoreB B팀(home)의 최종 타겟 점수 (-1 = 비활성)
 */
public record ScoreTargetContext(int targetScoreA, int targetScoreB) {

    /** 스코어 모드 비활성(일반 모드) 컨텍스트 */
    public static ScoreTargetContext none() {
        return new ScoreTargetContext(-1, -1);
    }

    /** 스코어 모드 활성 여부 */
    public boolean isScoreMode() {
        return targetScoreA >= 0 && targetScoreB >= 0;
    }

    /** A팀이 타겟 점수에 도달했는지 여부 */
    public boolean isTeamADone(int scoreA) {
        return scoreA >= targetScoreA;
    }

    /** B팀이 타겟 점수에 도달했는지 여부 */
    public boolean isTeamBDone(int scoreB) {
        return scoreB >= targetScoreB;
    }

    /** 양 팀 모두 타겟 점수에 도달했는지 여부 */
    public boolean bothDone(int scoreA, int scoreB) {
        return isTeamADone(scoreA) && isTeamBDone(scoreB);
    }
}
