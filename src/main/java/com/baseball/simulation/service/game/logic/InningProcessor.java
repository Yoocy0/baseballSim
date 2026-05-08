package com.baseball.simulation.service.game.logic;

import com.baseball.simulation.domain.BatterStatSnapshot;
import com.baseball.simulation.domain.PitcherStatSnapshot;
import com.baseball.simulation.domain.ScoreTargetContext;
import com.baseball.simulation.domain.WinControlContext;
import com.baseball.simulation.domain.dto.GameRecordDto;
import com.baseball.simulation.domain.dto.PlayerDto;
import com.baseball.simulation.service.game.logic.PlateAppearanceProcessor.PASimResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 반이닝(Half-Inning) 단위 시뮬레이션을 처리합니다.
 * <p>
 * 베이스 주자는 Long(선수 ID)으로 관리합니다. null = 공루.
 * <p>
 * 매 타석 종료 후:
 * - 홈인한 선수(scorerIds): BatterStatSnapshot.addRun() 호출 → 득점(runs) 반영
 * - 타자(batter): BatterStatSnapshot.applyPaResult() 호출 → 타석·타점·안타 등 반영
 * - 투수(pitcher): PitcherStatSnapshot.applyPaResult() 호출 → 투구수·실점·아웃 반영
 * <p>
 * [확률 보정 모드 — 우선순위]
 * <ol>
 *   <li>승/패 제어 모드 ({@link WinControlContext}):
 *       공격={@link ChaseOffenseService}, 수비={@link FateDefenseService}</li>
 *   <li>스코어 모드 ({@link ScoreTargetContext}):
 *       공수 통합={@link ScoreModeController}</li>
 *   <li>일반 모드: 보정 없음</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
public class InningProcessor {

    private final PlateAppearanceProcessor paProcessor;
    private final ChaseOffenseService      chaseOffenseService;
    private final FateDefenseService       fateDefenseService;
    private final ScoreModeController      scoreModeController;

    public InningSimResult process(
            Long gameId,
            int inning,
            boolean isTop,
            List<PlayerDto> battingTeam,
            List<PlayerDto> pitchingTeam,
            int startBattingIndex,
            int displayScoreA,
            int displayScoreB,
            boolean allowWalkOff,
            int awayScore,
            int homeScoreBeforeHalf,
            List<GameRecordDto> allRecords,
            Map<Long, BatterStatSnapshot>  batterStats,
            Map<Long, PitcherStatSnapshot> pitcherStats,
            WinControlContext winCtx,
            ScoreTargetContext scoreCtx
    ) {
        int halfInningSeq = inning * 2 + (isTop ? -1 : 0);
        String halfLabel  = inning + "회 " + (isTop ? "초" : "말");

        System.out.printf("%n==== %s ====%n", halfLabel);

        int  outs           = 0;
        int  runs           = 0;
        Long runnerOnFirst  = null;
        Long runnerOnSecond = null;
        Long runnerOnThird  = null;
        int  battingIndex   = startBattingIndex;

        PlayerDto pitcher = pitchingTeam.get(0);
        // TODO(part2): 이닝/투구 수에 따른 투수 교체 로직

        boolean lastChancePrinted = false;

        while (outs < 3) {
            PlayerDto batter     = battingTeam.get(battingIndex);
            int       runsBeforePA = runs;

            // ── 실시간 점수 계산 ──────────────────────────────────────────────
            int liveScoreA = isTop  ? displayScoreA + runs : displayScoreA;
            int liveScoreB = !isTop ? displayScoreB + runs : displayScoreB;

            // ── 모드별 보정 결정 ──────────────────────────────────────────────
            BattingMode         battingMode;
            DefensiveConstraint defConstraint;

            if (winCtx.isControlMode()) {
                // [모드 1] 승/패 제어 — ChaseOffenseService + FateDefenseService
                battingMode  = chaseOffenseService.computeBattingMode(
                        winCtx, inning, isTop, outs, liveScoreA, liveScoreB);
                defConstraint = fateDefenseService.computeConstraint(
                        winCtx, inning, isTop, liveScoreA, liveScoreB);

                if (battingMode == BattingMode.LAST_CHANCE && !lastChancePrinted) {
                    System.out.println("[시스템] ⚡ 기적 로직 가동! 이번 타석은 반드시 안타 이상!");
                    lastChancePrinted = true;
                } else if (battingMode == BattingMode.CHASE) {
                    System.out.println("[시스템] 승리 팀 보정 로직 가동 중...");
                }

            } else if (scoreCtx.isScoreMode()) {
                // [모드 2] 스코어 모드 — ScoreModeController
                int battingScore   = isTop ? liveScoreA : liveScoreB;
                int battingTarget  = isTop ? scoreCtx.targetScoreA() : scoreCtx.targetScoreB();
                int opponentScore  = isTop ? liveScoreB : liveScoreA;
                int opponentTarget = isTop ? scoreCtx.targetScoreB() : scoreCtx.targetScoreA();
                boolean opponentDone = opponentScore >= opponentTarget;

                battingMode   = scoreModeController.computeOffensiveMode(
                        battingScore, battingTarget, inning, outs);
                defConstraint = scoreModeController.evaluateScoreCondition(
                        battingScore, battingTarget, opponentScore, opponentDone);

                if (battingMode == BattingMode.LAST_CHANCE && !lastChancePrinted) {
                    System.out.println("[스코어 조작] ⚡ 기적 로직 가동! 타겟 점수 달성을 위해 안타를 보장합니다.");
                    lastChancePrinted = true;
                } else if (battingMode == BattingMode.CHASE) {
                    System.out.println("[스코어 조작] 타겟 점수 추격 보정 가동 중...");
                }

            } else {
                // [모드 3] 일반 모드
                battingMode   = BattingMode.NORMAL;
                defConstraint = DefensiveConstraint.none();
            }

            PASimResult pa = paProcessor.process(
                    gameId, halfInningSeq, inning, isTop,
                    batter, pitcher,
                    runnerOnFirst, runnerOnSecond, runnerOnThird,
                    outs, runs,
                    displayScoreA, displayScoreB,
                    battingMode,
                    defConstraint
            );

            // ── 반이닝 상태 갱신 ────────────────────────────────────────────
            outs           = pa.outs();
            runs           = pa.runs();
            runnerOnFirst  = pa.runnerOnFirst();
            runnerOnSecond = pa.runnerOnSecond();
            runnerOnThird  = pa.runnerOnThird();

            int runsInThisPA = runs - runsBeforePA;
            int pitchCount   = pa.pitchRecords().size();

            // ── PA-end 레코드에 실제 득점(0~4) 반영 ─────────────────────────
            List<GameRecordDto> paRecords = new ArrayList<>(pa.pitchRecords());
            if (!paRecords.isEmpty()) {
                int lastIdx = paRecords.size() - 1;
                GameRecordDto last = paRecords.get(lastIdx);
                if (last.paEnd()) {
                    paRecords.set(lastIdx, new GameRecordDto(
                            last.gameId(), last.inning(), last.pitcherId(), last.batterId(),
                            last.pitchSequence(), last.pitchResult(), last.paResult(),
                            last.paEnd(), last.bsoCount(), runsInThisPA
                    ));
                }
            }
            allRecords.addAll(paRecords);

            // ── 득점한 선수들 runs 업데이트 ─────────────────────────────────
            for (Long scorerId : pa.scorerIds()) {
                BatterStatSnapshot scorerSnap = batterStats.get(scorerId);
                if (scorerSnap != null) scorerSnap.addRun();
            }

            // ── 타자 인메모리 성적 업데이트 ─────────────────────────────────
            BatterStatSnapshot batterSnap = batterStats.get(batter.id());
            if (batterSnap != null) {
                batterSnap.applyPaResult(pa.paResultCode(), runsInThisPA);
            }

            // ── 투수 인메모리 성적 업데이트 ─────────────────────────────────
            PitcherStatSnapshot pitcherSnap = pitcherStats.get(pitcher.id());
            if (pitcherSnap != null) {
                pitcherSnap.applyPaResult(pa.paResultCode(), pitchCount, runsInThisPA);
            }

            // ── 끝내기 판정 (일반/승패 모드에서만 적용) ─────────────────────
            if (!scoreCtx.isScoreMode()
                    && allowWalkOff && !isTop && (homeScoreBeforeHalf + runs) > awayScore) {
                printInningEnd(halfLabel, outs, true);
                return new InningSimResult(runs, nextIndex(battingIndex, battingTeam), outs, true);
            }

            battingIndex = nextIndex(battingIndex, battingTeam);
        }

        printInningEnd(halfLabel, outs, false);
        return new InningSimResult(runs, battingIndex, outs, false);
    }

    private void printInningEnd(String halfLabel, int outs, boolean walkOff) {
        if (walkOff) {
            System.out.printf("==== %s 종료 (끝내기!) ====%n", halfLabel);
        } else {
            System.out.printf("==== %s 종료 (%d아웃) ====%n", halfLabel, outs);
        }
        System.out.println("================");
    }

    private int nextIndex(int current, List<PlayerDto> team) {
        return (current + 1) % team.size();
    }

    public record InningSimResult(
            int runs,
            int nextBattingIndex,
            int outsAdded,
            boolean walkOff
    ) {}
}
