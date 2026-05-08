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
     * @param gameId         경기 ID (GameRecordDto 생성용)
     * @param halfInningSeq  이닝-상하 순서값 (inning*2 - 1 = 초, inning*2 = 말)
     * @param inning         현재 이닝
     * @param isTop          초(true) / 말(false)
     * @param batter         타자 DTO
     * @param pitcher        투수 DTO
     * @param onFirst        1루 주자 존재 여부
     * @param onSecond       2루 주자 존재 여부
     * @param onThird        3루 주자 존재 여부
     * @param currentOuts    타석 시작 전 아웃 수
     * @param currentRuns    이번 반이닝 누적 득점 (타석 시작 전)
     * @param displayScoreA  A팀 표시 점수 (이닝 시작 시점 기준)
     * @param displayScoreB  B팀 표시 점수 (이닝 시작 시점 기준)
     * @return 타석 결과 (주자 상태, 아웃, 득점, 투구 기록 포함)
     */
    public PASimResult process(
            Long gameId,
            int halfInningSeq,
            int inning,
            boolean isTop,
            PlayerDto batter,
            PlayerDto pitcher,
            boolean onFirst,
            boolean onSecond,
            boolean onThird,
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

        // 타석 헤더를 먼저 출력 → 그 아래에 투구 내용이 이어짐
        System.out.printf("  [타자: %s | 투수: %s]%n", batter.name(), pitcher.name());

        while (true) {
            PitchOutcome outcome = pitchDecider.decide();
            String bsoCountBefore = String.format("B%d-S%d-O%d", balls, strikes, outs);

            String paResultCode = null;

            if (outcome.isStrikeCount()) {
                strikes++;
                if (strikes >= 3) {
                    outs++;
                    paResultCode = "STRIKEOUT";
                }
            } else if (outcome.isBallCount()) {
                balls++;
                if (balls >= 4) {
                    WalkState walked = applyWalk(onFirst, onSecond, onThird, runs);
                    onFirst  = walked.onFirst();
                    onSecond = walked.onSecond();
                    onThird  = walked.onThird();
                    runs     = walked.runs();
                    paResultCode = "WALK";
                }
            } else if (outcome.isFieldOut()) {
                // 타구 아웃: 삼진 아닌 범타 (그라운드 아웃, 플라이 아웃 등)
                outs++;
                paResultCode = "FIELD_OUT";
            } else if (outcome.isHit()) {
                HitState hit = applyHit(outcome, onFirst, onSecond, onThird, runs);
                onFirst  = hit.onFirst();
                onSecond = hit.onSecond();
                onThird  = hit.onThird();
                runs     = hit.runs();
                paResultCode = outcome.name(); // SINGLE, DOUBLE, TRIPLE, HOMERUN
            }

            String bsoCountAfter = String.format("B%d-S%d-O%d", balls, strikes, outs);

            // 투구 한 구 출력 (점수 미포함)
            System.out.printf(
                    "  %d구 %s | %s → %s%n",
                    pitchSequence, outcome.label(), bsoCountBefore, bsoCountAfter
            );

            pitchRecords.add(new GameRecordDto(
                    gameId, halfInningSeq,
                    pitcher.id(), batter.id(),
                    pitchSequence, outcome.name(),
                    paResultCode, paResultCode != null, bsoCountBefore
            ));

            if (paResultCode != null) {
                // 실시간 점수: 이닝 시작 점수 + 이번 반이닝 누적 득점
                int liveScoreA = isTop ? displayScoreA + runs : displayScoreA;
                int liveScoreB = isTop ? displayScoreB       : displayScoreB + runs;

                System.out.printf(
                        "  ▶ 결과: %s (%d:%d)%n%n",
                        paResultCode, liveScoreA, liveScoreB
                );

                return new PASimResult(
                        paResultCode,
                        onFirst, onSecond, onThird,
                        outs, runs,
                        pitchRecords
                );
            }

            pitchSequence++;
        }
    }

    // -----------------------------------------------------------------------
    // 주자 처리 헬퍼
    // -----------------------------------------------------------------------

    /**
     * 볼넷 처리: 주자를 한 베이스씩 밀어내고, 만루에서 볼넷이면 1점 득점.
     */
    private WalkState applyWalk(boolean onFirst, boolean onSecond, boolean onThird, int runs) {
        if (onFirst && onSecond && onThird) runs++;
        if (onFirst && onSecond) onThird = true;
        if (onFirst)             onSecond = true;
        onFirst = true;
        return new WalkState(onFirst, onSecond, onThird, runs);
    }

    /**
     * 안타 처리: 타격 종류에 따라 주자를 이동시키고 득점을 계산합니다.
     * TODO(part2): 주자 개인 발 빠름 속성(스피드)에 따른 진루 범위 차등화
     */
    private HitState applyHit(
            PitchOutcome hitType,
            boolean onFirst, boolean onSecond, boolean onThird,
            int runs
    ) {
        if (hitType == PitchOutcome.HOMERUN) {
            int scored = 1; // 타자 본인
            if (onFirst)  scored++;
            if (onSecond) scored++;
            if (onThird)  scored++;
            return new HitState(false, false, false, runs + scored);
        }

        int bases = switch (hitType) {
            case SINGLE -> 1;
            case DOUBLE -> 2;
            case TRIPLE -> 3;
            default     -> 0;
        };

        int newFirst = 0, newSecond = 0, newThird = 0;

        if (onThird && bases >= 1) runs++;
        if (onSecond) {
            if (bases >= 2) runs++;
            else            newThird = 1;
        }
        if (onFirst) {
            if (bases >= 3)      runs++;
            else if (bases == 2) newThird  = 1; // 1루 주자 → 3루 (2루타)
            else                 newSecond = 1; // 1루 주자 → 2루 (단타)
        }

        // 타자 배치
        if (bases == 1) newFirst  = 1;
        else if (bases == 2) newSecond = 1;
        else if (bases == 3) newThird  = 1;

        return new HitState(newFirst == 1, newSecond == 1, newThird == 1, runs);
    }

    // -----------------------------------------------------------------------
    // 내부 레코드
    // -----------------------------------------------------------------------

    private record WalkState(boolean onFirst, boolean onSecond, boolean onThird, int runs) {}
    private record HitState(boolean onFirst, boolean onSecond, boolean onThird, int runs) {}

    /**
     * 타석 시뮬레이션 결과
     *
     * @param paResultCode  STRIKEOUT / WALK / FIELD_OUT / SINGLE / DOUBLE / TRIPLE / HOMERUN
     * @param onFirst       1루 주자
     * @param onSecond      2루 주자
     * @param onThird       3루 주자
     * @param outs          타석 종료 후 누적 아웃 수
     * @param runs          타석 종료 후 이번 반이닝 누적 득점
     * @param pitchRecords  이번 타석의 투구 기록 목록
     */
    public record PASimResult(
            String paResultCode,
            boolean onFirst,
            boolean onSecond,
            boolean onThird,
            int outs,
            int runs,
            List<GameRecordDto> pitchRecords
    ) {}
}
