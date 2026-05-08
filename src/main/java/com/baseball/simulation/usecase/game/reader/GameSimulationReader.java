package com.baseball.simulation.usecase.game.reader;

import com.baseball.simulation.entity.Player;
import com.baseball.simulation.entity.Team;
import com.baseball.simulation.repository.PlayerRepository;
import com.baseball.simulation.repository.TeamRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GameSimulationReader {

    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;

    public Team getTeamByIdOrThrow(long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalStateException("team_id=" + teamId + " 팀이 존재하지 않습니다."));
    }

    public List<Player> readPlayersByTeamOrderByIdAsc(Team team) {
        return playerRepository.findByTeamOrderByIdAsc(team);
    }

    public List<Team> findAllTeams() {
        return teamRepository.findAll();
    }
}

