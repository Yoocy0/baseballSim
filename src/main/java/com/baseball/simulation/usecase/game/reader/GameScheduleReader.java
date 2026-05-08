package com.baseball.simulation.usecase.game.reader;

import com.baseball.simulation.entity.Game;
import com.baseball.simulation.repository.GameRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GameScheduleReader {

    private final GameRepository gameRepository;

    public List<Game> readRecentGames() {
        return gameRepository.findAllWithTeamsOrderByIdDesc();
    }
}

