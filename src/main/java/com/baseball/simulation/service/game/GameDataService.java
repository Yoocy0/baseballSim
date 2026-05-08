package com.baseball.simulation.service.game;

import com.baseball.simulation.domain.GameScheduleItem;
import com.baseball.simulation.domain.dto.GameInitDto;
import com.baseball.simulation.domain.dto.GameSimulationResultDto;
import com.baseball.simulation.domain.dto.PlayerDto;
import com.baseball.simulation.domain.dto.TeamDto;
import com.baseball.simulation.entity.Game;
import com.baseball.simulation.entity.Player;
import com.baseball.simulation.entity.Team;
import com.baseball.simulation.usecase.game.executor.GameSimulationExecutor;
import com.baseball.simulation.usecase.game.reader.GameScheduleReader;
import com.baseball.simulation.usecase.game.reader.GameSimulationReader;
import com.baseball.simulation.usecase.game.validator.GameScheduleValidator;
import com.baseball.simulation.usecase.game.validator.GameSimulationValidator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * DB 접근을 전담하는 서비스입니다.
 * Repository(usecase 계층)를 주입받으며, 파사드와의 통신은 DTO를 통해서만 합니다.
 * GameLogicService와 직접 의존 관계를 가지지 않습니다.
 */
@Service
@RequiredArgsConstructor
public class GameDataService {

    private final GameSimulationReader simulationReader;
    private final GameSimulationExecutor simulationExecutor;
    private final GameSimulationValidator simulationValidator;
    private final GameScheduleReader scheduleReader;
    private final GameScheduleValidator scheduleValidator;

    /**
     * 경기 시작에 필요한 팀/선수를 조회(없으면 생성)하고, 게임 엔티티를 생성해 GameInitDto로 반환합니다.
     * TODO(part2): 팀 ID를 파라미터로 받도록 변경 (현재는 1L, 2L 고정)
     */
    @Transactional
    public GameInitDto prepareGame() {
        Team teamA = simulationReader.getTeamByIdOrThrow(1L);
        Team teamB = simulationReader.getTeamByIdOrThrow(2L);
        simulationValidator.validateTeams(teamA, teamB);

        List<Player> teamAPlayers = simulationReader.readPlayersByTeamOrderByIdAsc(teamA);
        if (teamAPlayers.isEmpty()) {
            teamAPlayers = simulationExecutor.createPlayersForTeam(teamA, 1, 9);
        }

        List<Player> teamBPlayers = simulationReader.readPlayersByTeamOrderByIdAsc(teamB);
        if (teamBPlayers.isEmpty()) {
            teamBPlayers = simulationExecutor.createPlayersForTeam(teamB, 10, 18);
        }

        Game game = simulationExecutor.createGameInProgress(teamA, teamB);

        return new GameInitDto(
                game.getId(),
                new TeamDto(teamA.getId(), teamA.getName()),
                new TeamDto(teamB.getId(), teamB.getName()),
                toPlayerDtos(teamAPlayers),
                toPlayerDtos(teamBPlayers)
        );
    }

    /**
     * GameLogicService가 반환한 시뮬레이션 결과(DTO)를 DB에 저장합니다.
     * - GameRecord 전체 일괄 saveAll
     * - 최종 점수 반영 및 경기 상태 FINISHED 처리
     */
    @Transactional
    public void saveSimulationResult(GameSimulationResultDto result) {
        simulationExecutor.saveGameRecordDtos(result.records());
        simulationExecutor.finalizeGame(
                result.gameId(), result.finalScoreA(), result.finalScoreB(),
                result.inningScoresA(), result.inningScoresB()
        );
    }

    /**
     * 최근 경기 목록을 최신순으로 조회해 GameScheduleItem DTO 목록으로 반환합니다.
     */
    @Transactional(readOnly = true)
    public List<GameScheduleItem> fetchRecentGames() {
        List<Game> games = scheduleReader.readRecentGames();
        scheduleValidator.validate(games);
        return games.stream()
                .map(g -> new GameScheduleItem(
                        g.getId(),
                        g.getTeamB() != null ? g.getTeamB().getName() : "TeamB",
                        g.getScoreB(),
                        g.getTeamA() != null ? g.getTeamA().getName() : "TeamA",
                        g.getScoreA()
                ))
                .toList();
    }

    private List<PlayerDto> toPlayerDtos(List<Player> players) {
        return players.stream()
                .map(p -> new PlayerDto(p.getId(), p.getName()))
                .toList();
    }
}
