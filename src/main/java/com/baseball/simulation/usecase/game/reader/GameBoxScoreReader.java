package com.baseball.simulation.usecase.game.reader;

import com.baseball.simulation.entity.Game;
import com.baseball.simulation.entity.GameRecord;
import com.baseball.simulation.repository.GameRecordRepository;
import com.baseball.simulation.repository.GameRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 박스스코어 조회를 위한 Usecase 계층 Reader입니다.
 */
@Component
@RequiredArgsConstructor
public class GameBoxScoreReader {

    private final GameRepository gameRepository;
    private final GameRecordRepository gameRecordRepository;

    /**
     * 박스스코어 표시에 필요한 Game 엔티티를 팀 정보와 함께 조회합니다.
     */
    public Game findGameWithTeams(Long gameId) {
        return gameRepository.findByIdWithTeams(gameId)
                .orElseThrow(() -> new IllegalArgumentException("경기를 찾을 수 없습니다. gameId=" + gameId));
    }

    /**
     * 특정 경기의 타석 종료(paEnd=true) 레코드를 경기 진행 순서(id ASC)로 조회합니다.
     * 박스스코어의 타자/투수 통계 집계에 사용됩니다.
     */
    public List<GameRecord> findPaRecordsByGameId(Long gameId) {
        return gameRecordRepository.findPaEndByGameIdOrderByIdAsc(gameId);
    }

    /**
     * 모든 경기를 팀 정보와 함께 최신순으로 조회합니다.
     */
    public List<Game> findAllGamesWithTeams() {
        return gameRepository.findAllWithTeamsOrderByIdDesc();
    }
}
