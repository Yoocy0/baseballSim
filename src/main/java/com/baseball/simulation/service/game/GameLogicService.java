package com.baseball.simulation.service.game;

import com.baseball.simulation.domain.dto.GameInitDto;
import com.baseball.simulation.domain.dto.GameRecordDto;
import com.baseball.simulation.domain.dto.GameSimulationResultDto;
import com.baseball.simulation.domain.dto.PlayerDto;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.springframework.stereotype.Service;

/**
 * 순수 경기 시뮬레이션 로직을 담당하는 서비스입니다.
 * Repository 또는 다른 서비스를 주입받지 않으며, DTO를 통해서만 입출력합니다.
 * GameDataService와 직접 의존 관계를 가지지 않습니다.
 */
@Service
public class GameLogicService {

    private static final double SWING_PROBABILITY = 0.25;
    private static final double STRIKE_IF_NO_SWING_PROBABILITY = 0.75;
    private static final double OUT_IF_SWING_PROBABILITY = 0.8;
    // TODO(part2): 확률 상수를 선수 속성 기반 값으로 교체 (타율, 출루율, 장타율 등)
    // TODO(part2): 파울, 구종, 존 개념 추가
    // TODO(part3): 투수 구속/제구/피로도 반영

    private final Random random = new Random();

    /**
     * GameInitDto를 받아 9이닝(+연장) 경기를 메모리에서 시뮬레이션하고
     * 결과(최종 점수 + 전체 투구 기록)를 GameSimulationResultDto로 반환합니다.
     */
    public GameSimulationResultDto simulate(GameInitDto initDto) {
        List<GameRecordDto> allRecords = new ArrayList<>();

        int scoreA = 0;
        int scoreB = 0;
        int battingIndexA = 0;
        int battingIndexB = 0;
        int inning = 1;
        int outsA = 0;
        int outsB = 0;

        List<PlayerDto> teamAPlayers = initDto.teamAPlayers();
        List<PlayerDto> teamBPlayers = initDto.teamBPlayers();

        while (true) {
            int requiredOuts = inning * 3;

            // 상: Team A 공격, Team B 수비 (현재 누적 점수를 디스플레이 기준으로 전달)
            HalfInningResult top = simulateHalfInning(
                    initDto.gameId(), inning, true,
                    teamAPlayers, teamBPlayers,
                    battingIndexA, scoreA, scoreB,
                    false, 0, 0,
                    allRecords
            );
            scoreA += top.runs();
            battingIndexA = top.nextBattingIndex();
            outsA += top.outsAdded();

            // 9회 이후, 초 종료 시점에 홈팀이 이미 앞서 있으면 말 공격 없이 종료
            if (inning >= 9 && scoreB > scoreA) {
                break;
            }

            // 하: Team B 공격, Team A 수비 (초 종료 후 갱신된 scoreA를 기준으로 전달)
            HalfInningResult bottom = simulateHalfInning(
                    initDto.gameId(), inning, false,
                    teamBPlayers, teamAPlayers,
                    battingIndexB, scoreA, scoreB,
                    inning >= 9, scoreA, scoreB,
                    allRecords
            );
            scoreB += bottom.runs();
            battingIndexB = bottom.nextBattingIndex();
            outsB += bottom.outsAdded();

            // 말 공격 중 끝내기로 승부가 나면 즉시 종료
            if (bottom.walkOff()) {
                break;
            }

            // 이닝 아웃카운트(27 + n*3)를 양 팀 모두 채운 시점에 승부가 나면 종료
            if (inning >= 9 && outsA >= requiredOuts && outsB >= requiredOuts && scoreA != scoreB) {
                break;
            }

            inning++;
        }

        return new GameSimulationResultDto(initDto.gameId(), scoreA, scoreB, allRecords);
    }

    private HalfInningResult simulateHalfInning(
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
        int outs = 0;
        int runs = 0;
        boolean onFirst = false;
        boolean onSecond = false;
        boolean onThird = false;
        int battingIndex = startBattingIndex;

        while (outs < 3) {
            PlayerDto batter = battingTeam.get(battingIndex);
            PlayerDto pitcher = pitchingTeam.get(0); // 단순화: 1번 선수를 투수로 고정

            PAResult pa = simulatePlateAppearance(
                    gameId, inning, isTop,
                    batter, pitcher,
                    onFirst, onSecond, onThird,
                    outs, runs,
                    displayScoreA, displayScoreB,
                    allRecords
            );

            outs = pa.outs();
            runs = pa.runs();
            onFirst = pa.onFirst();
            onSecond = pa.onSecond();
            onThird = pa.onThird();

            // 말 공격에서 리드하는 순간 즉시 경기 종료(끝내기)
            if (allowWalkOff && !isTop && homeScoreBeforeHalf + runs > awayScore) {
                return new HalfInningResult(runs, battingIndex, outs, true);
            }

            battingIndex = (battingIndex + 1) % battingTeam.size();
        }

        return new HalfInningResult(runs, battingIndex, outs, false);
    }

    /**
     * 한 타석 시뮬레이션.
     * 투구 기록을 paBuffer에 쌓고, 타석 종료 시점에 allRecords로 일괄 이동합니다.
     * TODO(part2): 파울(2스트라이크 이후 카운트 유지), 희생번트, 몸에 맞는 공 등 세부 룰 확장
     */
    private PAResult simulatePlateAppearance(
            Long gameId,
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
            int displayScoreB,
            List<GameRecordDto> allRecords
    ) {
        int balls = 0;
        int strikes = 0;
        int outs = currentOuts;
        int runs = currentRuns;

        List<GameRecordDto> paBuffer = new ArrayList<>();
        int pitchSequence = 1;
        int halfInningSeq = inning * 2 + (isTop ? -1 : 0); // 이닝-상하 순서값

        while (true) {
            String pitchResult = decidePitchResult();
            String bsoCount = String.format("B%d-S%d-O%d", balls, strikes, outs);

            String paResult = null;

            if ("STRIKE".equals(pitchResult)) {
                strikes++;
                if (strikes >= 3) {
                    outs++;
                    paResult = "STRIKEOUT";
                }
            } else if ("BALL".equals(pitchResult)) {
                balls++;
                if (balls >= 4) {
                    WalkResult walkResult = handleWalk(onFirst, onSecond, onThird, runs);
                    onFirst = walkResult.onFirst();
                    onSecond = walkResult.onSecond();
                    onThird = walkResult.onThird();
                    runs = walkResult.runs();
                    paResult = "WALK";
                }
            } else if ("HIT".equals(pitchResult)) {
                HitResult hitResult = handleHit(onFirst, onSecond, onThird, runs);
                onFirst = hitResult.onFirst();
                onSecond = hitResult.onSecond();
                onThird = hitResult.onThird();
                runs = hitResult.runs();
                paResult = hitResult.paResult();
            }

            GameRecordDto record = new GameRecordDto(
                    gameId, halfInningSeq,
                    pitcher.id(), batter.id(),
                    pitchSequence, pitchResult,
                    paResult, paResult != null, bsoCount
            );
            paBuffer.add(record);

            if (paResult != null) {
                allRecords.addAll(paBuffer);

                System.out.printf(
                        "%d:%d 점수, %d회%s, 투수: %s, 타자: %s, 결과: %s, 카운트: %s%n",
                        displayScoreA, displayScoreB,
                        inning, isTop ? " 초" : " 말",
                        pitcher.name(), batter.name(),
                        paResult, bsoCount
                );

                return new PAResult(onFirst, onSecond, onThird, outs, runs);
            }

            if (outs >= 3) {
                allRecords.addAll(paBuffer);
                return new PAResult(onFirst, onSecond, onThird, outs, runs);
            }

            pitchSequence++;
        }
    }

    private String decidePitchResult() {
        if (random.nextDouble() < SWING_PROBABILITY) {
            return decideHitOrOut();
        }
        return random.nextDouble() < STRIKE_IF_NO_SWING_PROBABILITY ? "STRIKE" : "BALL";
    }

    private String decideHitOrOut() {
        return random.nextDouble() < OUT_IF_SWING_PROBABILITY ? "STRIKE" : "HIT";
    }

    private HitResult handleHit(boolean onFirst, boolean onSecond, boolean onThird, int runs) {
        double r = random.nextDouble();
        int bases;
        String paResult;

        if (r < 0.5) {
            bases = 1;
            paResult = "SINGLE";
        } else if (r < 0.8) {
            bases = 2;
            paResult = "DOUBLE";
        } else if (r < 0.95) {
            bases = 4;
            paResult = "HOMERUN";
        } else {
            bases = 3;
            paResult = "TRIPLE";
        }

        if (bases == 4) {
            int runnersScored = 1;
            if (onFirst) runnersScored++;
            if (onSecond) runnersScored++;
            if (onThird) runnersScored++;
            runs += runnersScored;
            onFirst = false;
            onSecond = false;
            onThird = false;
        } else {
            int newThird = 0;
            int newSecond = 0;
            int newFirst = 0;

            if (onThird && bases >= 1) runs++;
            if (onSecond) {
                if (bases >= 2) runs++;
                else newThird = 1;
            }
            if (onFirst) {
                if (bases >= 3) runs++;
                else if (bases == 2) newThird = 1;
                else newSecond = 1;
            }

            if (bases == 1) newFirst = 1;
            else if (bases == 2) newSecond = 1;
            else if (bases == 3) newThird = 1;

            onFirst = newFirst == 1;
            onSecond = newSecond == 1;
            onThird = newThird == 1;
        }

        return new HitResult(onFirst, onSecond, onThird, runs, paResult);
    }

    private WalkResult handleWalk(boolean onFirst, boolean onSecond, boolean onThird, int runs) {
        if (onFirst && onSecond && onThird) runs++;
        if (onSecond && onFirst) onThird = true;
        if (onFirst) onSecond = true;
        onFirst = true;
        return new WalkResult(onFirst, onSecond, onThird, runs);
    }

    private record HalfInningResult(int runs, int nextBattingIndex, int outsAdded, boolean walkOff) {
    }

    private record PAResult(boolean onFirst, boolean onSecond, boolean onThird, int outs, int runs) {
    }

    private record HitResult(boolean onFirst, boolean onSecond, boolean onThird, int runs, String paResult) {
    }

    private record WalkResult(boolean onFirst, boolean onSecond, boolean onThird, int runs) {
    }
}
