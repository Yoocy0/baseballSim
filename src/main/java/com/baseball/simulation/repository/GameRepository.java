package com.baseball.simulation.repository;

import com.baseball.simulation.entity.Game;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface GameRepository extends JpaRepository<Game, Long> {

    @Query("""
            select g
            from Game g
            left join fetch g.teamA
            left join fetch g.teamB
            order by g.id desc
            """)
    List<Game> findAllWithTeamsOrderByIdDesc();
}

