package com.baseball.simulation.service.game.logic;

import com.baseball.simulation.domain.BatterStatSnapshot;
import com.baseball.simulation.domain.PitcherStatSnapshot;
import com.baseball.simulation.domain.dto.GameRecordDto;
import com.baseball.simulation.domain.dto.PlayerDto;
import com.baseball.simulation.service.game.logic.PlateAppearanceProcessor.PASimResult;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 반이닝(Half-Inning) 단위 시뮬레이션을 처리합니다.
 * <p>
 * 매 타석 종료 후:
 * - BatterStatSnapshot: 타석 결과(paResultCode)·타점(runsInThisPA) 즉시 반영
 * - PitcherStatSnapshot: 투구수·피안타·실점·아웃(inningsX3) 즉시 반영
 * 이닝 시작/종료 배너와 구분선("================")을 출력합니다.
 */
@Component
@RequiredArgsConstructor
public class InningProcessor {

    private final PlateAppearanceProcessor paProcessor;

    /**
     * 반이닝을 시뮬레이션합니다.
     *
     * @param batterStats  인메모리 타자 성적 맵 (매 타석 결과 즉시 반영)
     * @param pitcherStats 인메모리 투수 성적 맵 (매 타석 결과 즉시 반영)
     */
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
            Map<Long, PitcherStatSnapshot> pitcherStats
    ) {
        int halfInningSeq = inning * 2 + (isTop ? -1 : 0);
        String halfLabel  = inning + "회 " + (isTop ? "초" : "말");

        System.out.printf("%n==== %s ====%n", halfLabel);

        int outs     = 0;
        int runs     = 0;
        boolean onFirst  = false;
        boolean onSecond = false;
        boolean onThird  = false;
        int battingIndex = startBattingIndex;

        // 이 반이닝의 투수 (항상 pitchingTeam.get(0))
        PlayerDto pitcher = pitchingTeam.get(0);
        // TODO(part2): 이닝/투구 수에 따른 투수 교체 로직

        while (outs < 3) {
            PlayerDto batter = battingTeam.get(battingIndex);
            int runsBeforePA = runs;

            PASimResult pa = paProcessor.process(
                    gameId, halfInningSeq, inning, isTop,
                    batter, pitcher,
                    onFirst, onSecond, onThird,
                    outs, runs,
                    displayScoreA, displayScoreB
            );

            allRecords.addAll(pa.pitchRecords());

            outs     = pa.outs();
            runs     = pa.runs();
            onFirst  = pa.onFirst();
            onSecond = pa.onSecond();
            onThird  = pa.onThird();

            int runsInThisPA = runs - runsBeforePA;
            int pitchCount   = pa.pitchRecords().size();

            // 타자 인메모리 성적 즉시 업데이트
            BatterStatSnapshot batterSnap = batterStats.get(batter.id());
            if (batterSnap != null) {
                batterSnap.applyPaResult(pa.paResultCode(), runsInThisPA);
            }

            // 투수 인메모리 성적 즉시 업데이트
            PitcherStatSnapshot pitcherSnap = pitcherStats.get(pitcher.id());
            if (pitcherSnap != null) {
                pitcherSnap.applyPaResult(pa.paResultCode(), pitchCount, runsInThisPA);
            }

            // 끝내기: 말 공격에서 홈팀이 리드하면 즉시 경기 종료
            if (allowWalkOff && !isTop && (homeScoreBeforeHalf + runs) > awayScore) {
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
