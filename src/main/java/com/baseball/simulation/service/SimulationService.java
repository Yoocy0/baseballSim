package com.baseball.simulation.service;

import com.baseball.simulation.entity.Game;
import com.baseball.simulation.entity.GameRecord;
import com.baseball.simulation.entity.Player;
import com.baseball.simulation.entity.Team;
import com.baseball.simulation.repository.GameRecordRepository;
import com.baseball.simulation.repository.GameRepository;
import com.baseball.simulation.repository.PlayerRepository;
import com.baseball.simulation.repository.TeamRepository;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SimulationService {

    private static final double SWING_PROBABILITY = 0.4;
    private static final double STRIKE_IF_NO_SWING_PROBABILITY = 0.75;
    private static final double OUT_IF_SWING_PROBABILITY = 0.8;

    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final GameRepository gameRepository;
    private final GameRecordRepository gameRecordRepository;

    private final Random random = new Random();

    @Transactional
    public void runSingleGame() {
        Team teamA = getTeamByFixedId(1L);
        Team teamB = getTeamByFixedId(2L);

        List<Player> teamAPlayers = createPlayersIfNotExists(teamA, 1, 9);
        List<Player> teamBPlayers = createPlayersIfNotExists(teamB, 10, 18);

        Game game = new Game();
        game.setTeamA(teamA);
        game.setTeamB(teamB);
        game.setScoreA(0);
        game.setScoreB(0);
        game.setStatus("IN_PROGRESS");
        gameRepository.save(game);

        simulateUntilWinner(game, teamAPlayers, teamBPlayers);

        game.setStatus("FINISHED");
        gameRepository.save(game);

        System.out.printf("최종 스코어: %d : %d%n", game.getScoreA(), game.getScoreB());
    }

    private Team getTeamByFixedId(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalStateException(
                        "team_id=" + teamId + " 팀이 존재하지 않습니다."
                ));
    }

    private List<Player> createPlayersIfNotExists(Team team, int startNumber, int endNumber) {
        List<Player> players = playerRepository.findByTeamOrderByIdAsc(team);
        if (!players.isEmpty()) {
            return players;
        }

        List<Player> created = new ArrayList<>();
        for (int i = startNumber; i <= endNumber; i++) {
            Player p = new Player();
            p.setName(team.getName() + " Player " + i);
            p.setTeam(team);
            created.add(playerRepository.save(p));
        }
        return created;
    }

    private void simulateUntilWinner(Game game, List<Player> teamAPlayers, List<Player> teamBPlayers) {
        int scoreA = 0;
        int scoreB = 0;

        int battingIndexA = 0;
        int battingIndexB = 0;
        int inning = 1;
        int outsA = 0;
        int outsB = 0;

        while (true) {
            int requiredOuts = inning * 3; // 9회=27, 연장 10회=30 ...

            // 상: Team A 공격, Team B 수비
            InningResult top = simulateHalfInning(game, inning, true, teamAPlayers, teamBPlayers, battingIndexA);
            scoreA += top.runs;
            battingIndexA = top.nextBattingIndex;
            outsA += top.outsAdded;

            game.setScoreA(scoreA);
            game.setScoreB(scoreB);
            gameRepository.save(game);

            // 9회 이후, 초 종료 시점에 홈팀(말 공격 팀)이 이미 앞서 있으면 말 공격 없이 종료
            if (inning >= 9 && scoreB > scoreA) {
                break;
            }

            // 하: Team B 공격, Team A 수비
            InningResult bottom = simulateHalfInning(
                    game,
                    inning,
                    false,
                    teamBPlayers,
                    teamAPlayers,
                    battingIndexB,
                    inning >= 9,
                    scoreA,
                    scoreB
            );
            scoreB += bottom.runs;
            battingIndexB = bottom.nextBattingIndex;
            outsB += bottom.outsAdded;

            game.setScoreA(scoreA);
            game.setScoreB(scoreB);
            gameRepository.save(game);

            // 말 공격 중 끝내기로 승부가 나면 즉시 종료
            if (bottom.walkOff) {
                break;
            }

            // 해당 이닝 아웃카운트(27 + n*3)까지 양 팀 모두 도달한 시점에 승부가 나면 종료
            if (inning >= 9 && outsA >= requiredOuts && outsB >= requiredOuts && scoreA != scoreB) {
                break;
            }

            inning++;
        }
    }

    private InningResult simulateHalfInning(
            Game game,
            int inning,
            boolean isTop,
            List<Player> battingTeam,
            List<Player> pitchingTeam,
            int startBattingIndex
    ) {
        return simulateHalfInning(game, inning, isTop, battingTeam, pitchingTeam, startBattingIndex, false, 0, 0);
    }

    private InningResult simulateHalfInning(
            Game game,
            int inning,
            boolean isTop,
            List<Player> battingTeam,
            List<Player> pitchingTeam,
            int startBattingIndex,
            boolean allowWalkOff,
            int awayScore,
            int homeScoreBeforeHalf
    ) {
        int outs = 0;
        int runs = 0;

        boolean onFirst = false;
        boolean onSecond = false;
        boolean onThird = false;

        int battingIndex = startBattingIndex;

        while (outs < 3) {
            Player batter = battingTeam.get(battingIndex);
            Player pitcher = pitchingTeam.get(0); // 단순화를 위해 1번 선수를 투수로 고정

            PlateAppearanceResult paResult = simulatePlateAppearance(game, inning, batter, pitcher, isTop,
                    onFirst, onSecond, onThird, outs, runs);

            outs = paResult.outs;
            runs = paResult.runs;
            onFirst = paResult.onFirst;
            onSecond = paResult.onSecond;
            onThird = paResult.onThird;

            // 말 공격에서 점수를 내는 순간 역전/리드하면 즉시 경기 종료(끝내기)
            if (allowWalkOff && !isTop && homeScoreBeforeHalf + runs > awayScore) {
                return new InningResult(runs, battingIndex, outs, true);
            }

            battingIndex = (battingIndex + 1) % battingTeam.size();
        }

        return new InningResult(runs, battingIndex, outs, false);
    }

    /**
     * 한 타석 시뮬레이션.
     * GameRecord는 타석 동안 List에 쌓고, 타석 종료 시점에만 saveAll.
     */
    private PlateAppearanceResult simulatePlateAppearance(
            Game game,
            int inning,
            Player batter,
            Player pitcher,
            boolean isTop,
            boolean onFirst,
            boolean onSecond,
            boolean onThird,
            int currentOuts,
            int currentRuns
    ) {
        int balls = 0;
        int strikes = 0;
        int outs = currentOuts;
        int runs = currentRuns;

        List<GameRecord> buffer = new ArrayList<>();
        int pitchSequence = 1;

        while (true) {
            String pitchResult = decidePitchResult();

            String bsoCount = String.format("B%d-S%d-O%d", balls, strikes, outs);

            GameRecord record = new GameRecord();
            record.setGame(game);
            record.setInning(inning * (isTop ? 2 : 2) + (isTop ? -1 : 0)); // 단순히 상/하를 구분하지 않고 동일 이닝 번호 사용
            record.setPitcher(pitcher);
            record.setBatter(batter);
            record.setPitchSequence(pitchSequence);
            record.setPitchResult(pitchResult);
            record.setBsoCount(bsoCount);
            record.setPaEnd(false);

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
                    // 볼넷으로 1루 진루 및 주자 밀어내기
                    WalkResult walkResult = handleWalk(onFirst, onSecond, onThird, runs);
                    onFirst = walkResult.onFirst;
                    onSecond = walkResult.onSecond;
                    onThird = walkResult.onThird;
                    runs = walkResult.runs;
                    paResult = "WALK";
                }
            } else if ("HIT".equals(pitchResult)) {
                HitResult hitResult = handleHit(onFirst, onSecond, onThird, runs);
                onFirst = hitResult.onFirst;
                onSecond = hitResult.onSecond;
                onThird = hitResult.onThird;
                runs = hitResult.runs;
                paResult = hitResult.paResult;
            }

            if (paResult != null) {
                record.setPaResult(paResult);
                record.setPaEnd(true);
                buffer.add(record);

                gameRecordRepository.saveAll(buffer);

                System.out.printf(
                        "%d:%d 점수, %d회%s, 투수: %s, 타자: %s, 결과: %s, 카운트: %s%n",
                        game.getScoreA(), game.getScoreB(),
                        inning,
                        isTop ? " 초" : " 말",
                        pitcher.getName(),
                        batter.getName(),
                        paResult,
                        bsoCount
                );

                return new PlateAppearanceResult(onFirst, onSecond, onThird, outs, runs);
            } else {
                buffer.add(record);
            }

            if (outs >= 3) {
                gameRecordRepository.saveAll(buffer);
                return new PlateAppearanceResult(onFirst, onSecond, onThird, outs, runs);
            }

            pitchSequence++;
        }
    }

    private String decidePitchResult() {
        // 50% 타격 / 비타격 시 스트라이크 65%, 볼 35%
        if (random.nextDouble() < SWING_PROBABILITY) {
            // 타격 상황
            return decideHitOrOut();
        } else {
            // 스트라이크 또는 볼
            return random.nextDouble() < STRIKE_IF_NO_SWING_PROBABILITY ? "STRIKE" : "BALL";
        }
    }

    private String decideHitOrOut() {
        // 타격 시 70% 아웃 / 30% 안타
        if (random.nextDouble() < OUT_IF_SWING_PROBABILITY) {
            return "STRIKE"; // 헛스윙/파울 등으로 단순 스트라이크 처리
        } else {
            return "HIT";
        }
    }

    private HitResult handleHit(boolean onFirst, boolean onSecond, boolean onThird, int runs) {
        double r = random.nextDouble();
        String paResult;

        int bases;
        if (r < 0.5) { // 단타 50
            bases = 1;
            paResult = "SINGLE";
        } else if (r < 0.8) { // 2루타 30
            bases = 2;
            paResult = "DOUBLE";
        } else if (r < 0.95) { // 홈런 15
            bases = 4;
            paResult = "HOMERUN";
        } else { // 3루타 5
            bases = 3;
            paResult = "TRIPLE";
        }

        if (bases == 4) {
            int runnersScored = 1; // 타자
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

            if (onThird) {
                if (bases >= 1) runs++;
            }
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
        if (onFirst && onSecond && onThird) {
            runs++;
        }
        if (onSecond && onFirst) {
            onThird = true;
        }
        if (onFirst) {
            onSecond = true;
        }
        onFirst = true;

        return new WalkResult(onFirst, onSecond, onThird, runs);
    }

    private record InningResult(int runs, int nextBattingIndex, int outsAdded, boolean walkOff) {
    }

    private record PlateAppearanceResult(
            boolean onFirst,
            boolean onSecond,
            boolean onThird,
            int outs,
            int runs
    ) {
    }

    private record HitResult(
            boolean onFirst,
            boolean onSecond,
            boolean onThird,
            int runs,
            String paResult
    ) {
    }

    private record WalkResult(
            boolean onFirst,
            boolean onSecond,
            boolean onThird,
            int runs
    ) {
    }
}

