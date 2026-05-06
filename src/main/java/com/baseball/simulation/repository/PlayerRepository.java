package com.baseball.simulation.repository;

import com.baseball.simulation.entity.Player;
import com.baseball.simulation.entity.Team;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerRepository extends JpaRepository<Player, Long> {

    List<Player> findByTeamOrderByIdAsc(Team team);
}

