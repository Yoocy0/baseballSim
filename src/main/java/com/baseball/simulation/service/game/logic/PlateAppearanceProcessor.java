package com.baseball.simulation.service.game.logic;

import com.baseball.simulation.domain.dto.GameRecordDto;
import com.baseball.simulation.domain.dto.PlayerDto;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 타석(Plate Appearance) 단위 시뮬레이션을 처리합니다.
 * <p>
 * [출력 형식]
 *   [타자: XXX | 투수: XXX]
 *   1구 SWING_MS  | B0-S0-O0 → B0-S1-O0
 *   2구 CALLED_S  | B0-S1-O0 → B0-S2-O0
 *   ▶ 결과: STRIKEOUT (0:0)
 * <p>
 * 베이스 주자는 Long(선수 ID)으로 관리합니다. null = 주자 없음.
 * <p>
 * TODO(part2): 파울(2스트라이크 이후 카운트 유지) 추가
 * TODO(part2): 희생번트, 희생플라이 추가
 * TODO(part2): FIELD_OUT 시 주자 진루/득점 세부 처리 (현재는 주자 고정)
 */
@Component
@RequiredArgsConstructor
public class PlateAppearanceProcessor {

    private final PitchDecider pitchDecider;

    /**
     * 한 타석을 시뮬레이션합니다.
     *
     * @param runnerOnFirst  1루 주자 선수 ID (null = 공루)
     * @param runnerOnSecond 2루 주자 선수 ID (null = 공루)
     * @param runnerOnThird  3루 주자 선수 ID (null = 공루)
     */
    public PASimResult process(
            Long gameId,
            int halfInningSeq,
            int inning,
            boolean isTop,
            PlayerDto batter,
            PlayerDto pitcher,
            Long runnerOnFirst,
            Long runnerOnSecond,
            Long runnerOnThird,
            int currentOuts,
            int currentRuns,
            int displayScoreA,
            int displayScoreB
    ) {
        int balls   = 0;
        int strikes = 0;
        int outs    = currentOuts;
        int runs    = currentRuns;

        List<GameRecordDto> pitchRecords = new ArrayList<>();
        int pitchSequence = 1;

        System.out.printf("  [타자: %s | 투수: %s]%n", batter.name(), pitcher.name());

        while (true) {
            PitchOutcome outcome = pitchDecider.decide();
            String bsoCountBefore = String.format("B%d-S%d-O%d", balls, strikes, outs);

            String   paResultCode = null;
            List<Long> scorerIds  = new ArrayList<>();

            if (outcome.isStrikeCount()) {
                strikes++;
                if (strikes >= 3) {
                    outs++;
                    paResultCode = "STRIKEOUT";
                }
            } else if (outcome.isBallCount()) {
                balls++;
                if (balls >= 4) {
                    WalkResult walked = applyWalk(batter.id(), runnerOnFirst, runnerOnSecond, runnerOnThird, runs);
                    runnerOnFirst  = walked.runnerOnFirst();
                    runnerOnSecond = walked.runnerOnSecond();
                    runnerOnThird  = walked.runnerOnThird();
                    runs           = walked.runs();
                    scorerIds      = walked.scorerIds();
                    paResultCode   = "WALK";
                }
            } else if (outcome.isFieldOut()) {
                outs++;
                paResultCode = "FIELD_OUT";
            } else if (outcome.isHit()) {
                HitResult hit = applyHit(outcome, batter.id(), runnerOnFirst, runnerOnSecond, runnerOnThird, runs);
                runnerOnFirst  = hit.runnerOnFirst();
                runnerOnSecond = hit.runnerOnSecond();
                runnerOnThird  = hit.runnerOnThird();
                runs           = hit.runs();
                scorerIds      = hit.scorerIds();
                paResultCode   = outcome.name();
            }

            String bsoCountAfter = String.format("B%d-S%d-O%d", balls, strikes, outs);
            System.out.printf("  %d구 %s | %s → %s%n",
                    pitchSequence, outcome.label(), bsoCountBefore, bsoCountAfter);

            pitchRecords.add(new GameRecordDto(
                    gameId, halfInningSeq,
                    pitcher.id(), batter.id(),
                    pitchSequence, outcome.name(),
                    paResultCode, paResultCode != null, bsoCountBefore,
                    paResultCode != null ? 0 : null   // paEnd=true: 0 초기화 후 InningProcessor에서 실제값 반영
            ));

            if (paResultCode != null) {
                int liveScoreA = isTop ? displayScoreA + runs : displayScoreA;
                int liveScoreB = isTop ? displayScoreB       : displayScoreB + runs;
                System.out.printf("  ▶ 결과: %s (%d:%d)%n%n", paResultCode, liveScoreA, liveScoreB);

                return new PASimResult(
                        paResultCode,
                        runnerOnFirst, runnerOnSecond, runnerOnThird,
                        outs, runs, scorerIds,
                        pitchRecords
                );
            }

            pitchSequence++;
        }
    }

    // -----------------------------------------------------------------------
    // 볼넷 주자 처리
    // -----------------------------------------------------------------------

    /**
     * 볼넷: 강제 진루 규칙에 따라 주자를 밀어냅니다.
     * 만루에서만 3루 주자가 홈인합니다.
     */
    private WalkResult applyWalk(
            Long batterId,
            Long runnerOnFirst, Long runnerOnSecond, Long runnerOnThird,
            int runs
    ) {
        List<Long> scorers = new ArrayList<>();

        if (runnerOnFirst != null && runnerOnSecond != null && runnerOnThird != null) {
            // 만루 볼넷: 3루 주자 강제 홈인
            scorers.add(runnerOnThird);
            runs++;
            runnerOnThird  = runnerOnSecond;
            runnerOnSecond = runnerOnFirst;
        } else if (runnerOnFirst != null && runnerOnSecond != null) {
            // 1·2루: 2루→3루, 1루→2루 (강제 진루)
            runnerOnThird  = runnerOnSecond;
            runnerOnSecond = runnerOnFirst;
        } else if (runnerOnFirst != null) {
            // 1루만 or 1·3루: 1루→2루 (강제 진루)
            runnerOnSecond = runnerOnFirst;
        }
        // 2루만·3루만·공루: 주자 이동 없음
        runnerOnFirst = batterId;

        return new WalkResult(runnerOnFirst, runnerOnSecond, runnerOnThird, runs, scorers);
    }

    // -----------------------------------------------------------------------
    // 안타 주자 처리
    // -----------------------------------------------------------------------

    /**
     * 안타 처리: 타격 종류에 따라 주자를 이동시키고 홈인한 선수 ID 목록을 반환합니다.
     * TODO(part2): 주자 발 빠름 속성(스피드)에 따른 진루 범위 차등화
     */
    private HitResult applyHit(
            PitchOutcome hitType,
            Long batterId,
            Long runnerOnFirst, Long runnerOnSecond, Long runnerOnThird,
            int runs
    ) {
        List<Long> scorers = new ArrayList<>();

        if (hitType == PitchOutcome.HOMERUN) {
            // 홈런: 모든 주자 + 타자 홈인
            if (runnerOnFirst  != null) { scorers.add(runnerOnFirst);  runs++; }
            if (runnerOnSecond != null) { scorers.add(runnerOnSecond); runs++; }
            if (runnerOnThird  != null) { scorers.add(runnerOnThird);  runs++; }
            scorers.add(batterId); runs++;   // 타자 본인 홈인
            return new HitResult(null, null, null, runs, scorers);
        }

        int bases = switch (hitType) {
            case SINGLE -> 1;
            case DOUBLE -> 2;
            case TRIPLE -> 3;
            default     -> 0;
        };

        Long newFirst = null, newSecond = null, newThird = null;

        // 3루 주자: 단타 이상이면 홈인
        if (runnerOnThird != null) {
            if (bases >= 1) { scorers.add(runnerOnThird); runs++; }
            else             newThird = runnerOnThird;
        }

        // 2루 주자: 2루타 이상이면 홈인, 단타면 3루
        if (runnerOnSecond != null) {
            if (bases >= 2) { scorers.add(runnerOnSecond); runs++; }
            else             newThird = runnerOnSecond;
        }

        // 1루 주자: 3루타 이상이면 홈인, 2루타면 3루, 단타면 2루
        if (runnerOnFirst != null) {
            if (bases >= 3)      { scorers.add(runnerOnFirst); runs++; }
            else if (bases == 2)   newThird  = runnerOnFirst;  // 2루타: 1루→3루
            else                   newSecond = runnerOnFirst;  // 단타:  1루→2루
        }

        // 타자 배치
        if      (bases == 1) newFirst  = batterId;
        else if (bases == 2) newSecond = batterId;
        else if (bases == 3) newThird  = batterId;

        return new HitResult(newFirst, newSecond, newThird, runs, scorers);
    }

    // -----------------------------------------------------------------------
    // 내부 레코드
    // -----------------------------------------------------------------------

    private record WalkResult(
            Long runnerOnFirst, Long runnerOnSecond, Long runnerOnThird,
            int runs, List<Long> scorerIds
    ) {}

    private record HitResult(
            Long runnerOnFirst, Long runnerOnSecond, Long runnerOnThird,
            int runs, List<Long> scorerIds
    ) {}

    /**
     * 타석 시뮬레이션 결과
     *
     * @param paResultCode  STRIKEOUT / WALK / FIELD_OUT / SINGLE / DOUBLE / TRIPLE / HOMERUN
     * @param runnerOnFirst  타석 종료 후 1루 주자 ID (null=공루)
     * @param runnerOnSecond 타석 종료 후 2루 주자 ID
     * @param runnerOnThird  타석 종료 후 3루 주자 ID
     * @param outs           타석 종료 후 누적 아웃 수
     * @param runs           타석 종료 후 이번 반이닝 누적 득점
     * @param scorerIds      이 타석에서 홈인한 선수 ID 목록 (득점자 추적)
     * @param pitchRecords   이번 타석의 투구 기록 목록
     */
    public record PASimResult(
            String paResultCode,
            Long runnerOnFirst,
            Long runnerOnSecond,
            Long runnerOnThird,
            int outs,
            int runs,
            List<Long> scorerIds,
            List<GameRecordDto> pitchRecords
    ) {}
}
