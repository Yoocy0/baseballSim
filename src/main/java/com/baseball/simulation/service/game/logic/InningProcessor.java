package com.baseball.simulation.service.game.logic;

import com.baseball.simulation.domain.dto.GameRecordDto;
import com.baseball.simulation.domain.dto.PlayerDto;
import com.baseball.simulation.service.game.logic.PlateAppearanceProcessor.PASimResult;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 반이닝(Half-Inning) 단위 시뮬레이션을 처리합니다.
 * <p>
 * 3아웃이 될 때까지 PlateAppearanceProcessor를 반복 호출하며,
 * 이닝 시작/종료 배너와 아웃 카운트 구분선("================")을 출력합니다.
 * <p>
 * [출력 예시]
 * ==== 2회 초 ====
 *   [타자: 장두성 | 투수: 안우진]
 *   1구 SWING_MS  | B0-S0-O0 → B0-S1-O0
 *   ▶ 결과: STRIKEOUT (0:0)
 *   ...
 * ==== 2회 초 종료 (3아웃) ====
 * ================
 */
@Component
@RequiredArgsConstructor
public class InningProcessor {

    private final PlateAppearanceProcessor paProcessor;

    /**
     * 반이닝을 시뮬레이션합니다.
     *
     * @param gameId             경기 ID
     * @param inning             현재 이닝
     * @param isTop              초(true) / 말(false)
     * @param battingTeam        공격팀 선수 목록
     * @param pitchingTeam       수비팀 선수 목록
     * @param startBattingIndex  타순 시작 인덱스
     * @param displayScoreA      A팀 이닝 시작 점수 (이닝 내 실시간 점수 계산용)
     * @param displayScoreB      B팀 이닝 시작 점수
     * @param allowWalkOff       끝내기 허용 여부 (말 공격에서 리드 시 즉시 종료)
     * @param awayScore          끝내기 판단 기준 원정팀 점수
     * @param homeScoreBeforeHalf 끝내기 판단 기준 홈팀 시작 점수
     * @param allRecords         전체 투구 기록 누적 리스트 (이닝 내 기록 추가됨)
     * @return 반이닝 결과
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
            List<GameRecordDto> allRecords
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

        while (outs < 3) {
            PlayerDto batter  = battingTeam.get(battingIndex);
            PlayerDto pitcher = pitchingTeam.get(0); // TODO(part2): 이닝/투구 수에 따른 투수 교체 로직

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

            // 끝내기: 말 공격에서 홈팀이 역전 또는 추가 득점으로 리드하면 즉시 경기 종료
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
    }

    private int nextIndex(int current, List<PlayerDto> team) {
        return (current + 1) % team.size();
    }

    /**
     * 반이닝 시뮬레이션 결과
     *
     * @param runs             이번 반이닝 득점
     * @param nextBattingIndex 다음 이닝 시작 타순 인덱스
     * @param outsAdded        이번 반이닝 아웃 수 (정규: 3)
     * @param walkOff          끝내기 여부
     */
    public record InningSimResult(
            int runs,
            int nextBattingIndex,
            int outsAdded,
            boolean walkOff
    ) {}
}
