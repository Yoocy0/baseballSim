package com.baseball.simulation.service.game.logic;

import org.springframework.stereotype.Component;

/**
 * 스코어 모드 컨트롤러 — 양 팀의 타겟 점수를 기반으로 공격 보정과 득점 상한을 결정합니다.
 * <p>
 * <b>공격 보정 (computeOffensiveMode)</b>
 * <pre>
 * 현재점수 ≥ 타겟  → NORMAL  (타겟 달성, 보정 불필요)
 * 9회 이상 + 2아웃  → LAST_CHANCE  (기적 로직: 100% 안타 강제)
 * 그 외            → CHASE  (추격 로직: 안타 확률 대폭 상향)
 * </pre>
 * <p>
 * <b>득점 상한 (evaluateScoreCondition)</b>
 * <pre>
 * 현재점수 ≥ 타겟            → cap(0)       완전 동결 (타겟 초과 방지)
 * 상대 미달성 + 현재팀이 뒤처짐 → cap(동점까지) 연장 유도 (역전 방지)
 * 그 외                      → cap(runsNeeded)  타겟까지만 득점 허용
 * </pre>
 * <p>
 * 이 클래스는 {@link InningProcessor}에 주입되어 사용되며,
 * 다른 서비스·유즈케이스에서도 재주입하여 독립적으로 활용할 수 있습니다.
 */
@Component
public class ScoreModeController {

    /**
     * 공격 팀의 현재 점수와 타겟을 비교하여 {@link BattingMode}를 결정합니다.
     *
     * @param currentScore 공격 팀 실시간 점수
     * @param targetScore  공격 팀 타겟 점수
     * @param inning       현재 이닝
     * @param outs         현재 아웃카운트
     */
    public BattingMode computeOffensiveMode(
            int currentScore, int targetScore, int inning, int outs
    ) {
        if (currentScore >= targetScore) return BattingMode.NORMAL;
        if (inning >= 9 && outs == 2)   return BattingMode.LAST_CHANCE;
        return BattingMode.CHASE;
    }

    /**
     * 공격 팀의 이번 타석 득점 상한을 계산합니다.
     * <p>
     * [Case 1] 타겟 달성 → cap(0) — 완전 동결<br>
     * [Case 2] 비상 연장 시나리오 → cap(동점) — 상대 미달성+현재팀이 뒤처질 때 역전 방지<br>
     * [Case 3] 일반 추격 → cap(runsNeeded) — 타겟 초과 방지
     *
     * @param currentScore        공격 팀 실시간 점수
     * @param targetScore         공격 팀 타겟 점수
     * @param opponentCurrentScore 상대 팀 실시간 점수
     * @param opponentReachedTarget 상대 팀이 타겟을 달성했는지 여부
     */
    public DefensiveConstraint evaluateScoreCondition(
            int currentScore, int targetScore,
            int opponentCurrentScore, boolean opponentReachedTarget
    ) {
        // Case 1: 타겟 달성 → 완전 동결
        if (currentScore >= targetScore) {
            return DefensiveConstraint.cap(0);
        }

        int runsNeeded = targetScore - currentScore;

        // Case 2: 비상 연장 시나리오
        // 상대도 타겟 미달성 + 현재 팀이 점수상 뒤처지고 있음
        // → 역전하지 않도록 동점까지만 허용 (연장전으로 유도)
        if (!opponentReachedTarget && currentScore < opponentCurrentScore) {
            int runsToTie = opponentCurrentScore - currentScore;
            return DefensiveConstraint.cap(Math.min(runsNeeded, runsToTie));
        }

        // Case 3: 일반 추격 → 타겟 초과 방지
        return DefensiveConstraint.cap(runsNeeded);
    }
}
