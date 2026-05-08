package com.baseball.simulation.repository;

import com.baseball.simulation.entity.Game;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GameRepository extends JpaRepository<Game, Long> {

    @Query("""
            select g from Game g
            left join fetch g.teamA
            left join fetch g.teamB
            order by g.id desc
            """)
    List<Game> findAllWithTeamsOrderByIdDesc();

    /** 박스스코어 조회: 특정 경기를 팀 정보와 함께 로드합니다. */
    @Query("""
            select g from Game g
            left join fetch g.teamA
            left join fetch g.teamB
            where g.id = :id
            """)
    Optional<Game> findByIdWithTeams(@Param("id") Long id);
}
