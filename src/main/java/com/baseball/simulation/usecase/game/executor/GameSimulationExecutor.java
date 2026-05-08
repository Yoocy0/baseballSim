package com.baseball.simulation.usecase.game.executor;

import com.baseball.simulation.domain.dto.GameRecordDto;
import com.baseball.simulation.entity.Game;
import com.baseball.simulation.entity.GameRecord;
import com.baseball.simulation.entity.Player;
import com.baseball.simulation.entity.Team;
import com.baseball.simulation.repository.GameRecordRepository;
import com.baseball.simulation.repository.GameRepository;
import com.baseball.simulation.repository.PlayerRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GameSimulationExecutor {

    private final PlayerRepository playerRepository;
    private final GameRepository gameRepository;
    private final GameRecordRepository gameRecordRepository;

    /**
     * 플레이어가 없을 때만 생성합니다.
     */
    public List<Player> createPlayersForTeam(Team team, int startNumber, int endNumber) {
        List<Player> created = new ArrayList<>();
        for (int i = startNumber; i <= endNumber; i++) {
            Player p = new Player();
            p.setName(team.getName() + " Player " + i);
            p.setTeam(team);
            created.add(playerRepository.save(p));
        }
        return created;
    }

    public Game createGameInProgress(Team teamA, Team teamB) {
        Game game = new Game();
        game.setTeamA(teamA);
        game.setTeamB(teamB);
        game.setScoreA(0);
        game.setScoreB(0);
        game.setStatus("IN_PROGRESS");
        return gameRepository.save(game);
    }

    /**
     * 경기 최종 점수 반영 및 상태를 FINISHED로 변경합니다.
     * GameLogicService에서 반환된 GameSimulationResultDto의 값을 받아 저장합니다.
     */
    public void finalizeGame(Long gameId, int scoreA, int scoreB) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalStateException("game_id=" + gameId + " 경기가 존재하지 않습니다."));
        game.setScoreA(scoreA);
        game.setScoreB(scoreB);
        game.setStatus("FINISHED");
        gameRepository.save(game);
    }

    /**
     * GameLogicService에서 반환된 GameRecordDto 목록을 엔티티로 변환해 일괄 저장합니다.
     * getReferenceById를 사용해 프록시 참조만 생성하므로 추가 SELECT가 발생하지 않습니다.
     * TODO(part2): 저장 전략 개선 (청크 단위 분할 저장 등) 고려
     */
    public void saveGameRecordDtos(List<GameRecordDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return;
        }
        List<GameRecord> entities = dtos.stream()
                .map(dto -> {
                    GameRecord r = new GameRecord();
                    r.setGame(gameRepository.getReferenceById(dto.gameId()));
                    r.setPitcher(playerRepository.getReferenceById(dto.pitcherId()));
                    r.setBatter(playerRepository.getReferenceById(dto.batterId()));
                    r.setInning(dto.inning());
                    r.setPitchSequence(dto.pitchSequence());
                    r.setPitchResult(dto.pitchResult());
                    r.setPaResult(dto.paResult());
                    r.setPaEnd(dto.paEnd());
                    r.setBsoCount(dto.bsoCount());
                    return r;
                })
                .toList();
        gameRecordRepository.saveAll(entities);
    }
}
