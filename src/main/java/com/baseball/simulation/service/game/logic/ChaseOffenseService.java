package com.baseball.simulation.service.game.logic;

import com.baseball.simulation.domain.WinControlContext;
import org.springframework.stereotype.Component;

/**
 * 추격 공격 서비스 — 승/패 제어 모드에서 승리 팀이 지고 있을 때 타격 결과를 강화합니다.
 * <p>
 * <b>발동 조건</b>
 * <ol>
 *   <li>승/패 제어 모드 ({@link WinControlContext#isControlMode()} = true)</li>
 *   <li>현재 반이닝이 승리 팀의 공격 이닝</li>
 *   <li>승리 팀이 현재 점수상 지고 있는 상태</li>
 * </ol>
 * <p>
 * <b>반환 모드</b>
 * <pre>
 * LAST_CHANCE  — 9회 이상 + 2아웃 → 해당 타석 100% 안타 이상 강제 (기적 로직)
 * CHASE        — 그 외 → 아웃 확률 대폭 감소, 안타/출루 확률 대폭 상향 (추격 로직)
 * NORMAL       — 발동 조건 미충족
 * </pre>
 * <p>
 * 이 클래스는 {@link InningProcessor}에 주입되어 사용되며,
 * 다른 서비스·유즈케이스에서도 재주입하여 독립적으로 활용할 수 있습니다.
 * 콘솔 출력은 이 클래스의 책임이 아니며 호출부({@link InningProcessor})에서 담당합니다.
 */
@Component
public class ChaseOffenseService {

    /**
     * 현재 타석에 적용할 공격 모드를 계산합니다.
     *
     * @param winCtx     승/패 제어 컨텍스트
     * @param inning     현재 이닝 번호 (1~)
     * @param isTop      초(true) / 말(false)
     * @param outs       현재 반이닝 누적 아웃 수
     * @param liveScoreA 이닝 중 실시간 A팀(away) 점수
     * @param liveScoreB 이닝 중 실시간 B팀(home) 점수
     * @return {@link BattingMode#NORMAL} / {@link BattingMode#CHASE} / {@link BattingMode#LAST_CHANCE}
     */
    public BattingMode computeBattingMode(
            WinControlContext winCtx,
            int inning,
            boolean isTop,
            int outs,
            int liveScoreA,
            int liveScoreB
    ) {
        // ① 일반 모드 or 승리 팀이 공격 이닝이 아님 or 지고 있지 않음 → 보정 불필요
        if (!winCtx.isControlMode()
                || !winCtx.isWinnerBatting(isTop)
                || !winCtx.isWinnerLosing(liveScoreA, liveScoreB)) {
            return BattingMode.NORMAL;
        }

        // ② 9회 이상 + 2아웃 → 기적 로직 (100% 안타 이상 강제)
        if (outs == 2 && inning >= 9) {
            return BattingMode.LAST_CHANCE;
        }

        // ③ 그 외 → 추격 로직 (아웃 확률 대폭 감소)
        return BattingMode.CHASE;
    }
}
