package com.baseball.simulation.service.game.logic;

import com.baseball.simulation.domain.WinControlContext;
import org.springframework.stereotype.Component;

/**
 * 운명 방어 서비스 — 승/패 제어 모드에서 승리 팀 수비 시 상대 팀의 타격 결과를 제한합니다.
 * <p>
 * <b>발동 조건</b>
 * <ol>
 *   <li>승/패 제어 모드({@link WinControlContext#isControlMode()} = true)</li>
 *   <li>9회 이상 ({@code inning >= 9})</li>
 *   <li>현재 반이닝이 상대 팀(패배 팀)의 공격 이닝</li>
 *   <li>승리 팀이 리드하거나 동점 ({@code winnerLead >= 0})</li>
 * </ol>
 * <p>
 * <b>점수차별 허용 상한</b>
 * <pre>
 * 리드 4+  → 무제한(자유 시뮬레이션)
 * 리드 3   → maxRuns = 3  (그랜드슬램 차단)
 * 리드 2   → maxRuns = 2  (역전 차단, 동점 허용)
 * 리드 1   → maxRuns = 1  (역전 차단, 동점 허용)
 * 동점     → maxRuns = 0  (어떤 득점도 불허)
 * 끝내기위험(말+9회+리드0~1) → maxRuns = 0 강제
 * </pre>
 * <p>
 * 이 클래스는 {@link InningProcessor}에 주입되어 사용되며,
 * 다른 서비스나 유즈케이스에서도 재주입하여 독립적으로 활용할 수 있습니다.
 */
@Component
public class FateDefenseService {

    /**
     * 현재 타석에 적용할 수비 제약을 계산합니다.
     * <p>
     * 9회 미만이거나 발동 조건을 충족하지 않으면 {@link DefensiveConstraint#none()}을 반환합니다.
     *
     * @param winCtx     승/패 제어 컨텍스트
     * @param inning     현재 이닝 번호 (1~)
     * @param isTop      초(true) / 말(false)
     * @param liveScoreA 이닝 중 실시간 A팀(away) 점수
     * @param liveScoreB 이닝 중 실시간 B팀(home) 점수
     * @return 이번 타석에 적용할 {@link DefensiveConstraint}
     */
    public DefensiveConstraint computeConstraint(
            WinControlContext winCtx,
            int inning,
            boolean isTop,
            int liveScoreA,
            int liveScoreB
    ) {
        // ① 일반 모드이거나 9회 미만이면 제약 없음
        if (!winCtx.isControlMode() || inning < 9) return DefensiveConstraint.none();

        // ② 승리 팀이 공격 중이면 수비 제약 불필요 (공격 측 BattingMode가 담당)
        if (winCtx.isWinnerBatting(isTop)) return DefensiveConstraint.none();

        // ③ 승리 팀의 현재 리드 점수 계산
        int winnerLead = computeWinnerLead(winCtx, liveScoreA, liveScoreB);

        // ④ 끝내기 방지 조건: 말 공격 + 리드 0~1 (inning >= 9는 ①에서 이미 확인)
        boolean walkOffRisk = !isTop && winnerLead >= 0 && winnerLead <= 1;

        return DefensiveConstraint.of(winnerLead, walkOffRisk);
    }

    /**
     * 승리 지정 팀의 리드 점수를 계산합니다.
     *
     * @return 양수 = 승리 팀 리드, 0 = 동점, 음수 = 승리 팀 열세
     */
    private int computeWinnerLead(WinControlContext winCtx, int liveScoreA, int liveScoreB) {
        if (winCtx.winnerTeamId().equals(winCtx.teamAId())) {
            return liveScoreA - liveScoreB;
        }
        return liveScoreB - liveScoreA;
    }
}
