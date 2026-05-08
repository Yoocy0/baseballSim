package com.baseball.simulation.repository;

import com.baseball.simulation.entity.Player;
import com.baseball.simulation.entity.Team;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlayerRepository extends JpaRepository<Player, Long> {

    List<Player> findByTeamOrderByIdAsc(Team team);

    /**
     * 타자 순위 출력용: 지정된 ID 목록의 Player와 Team을 한 번에 Fetch Join으로 조회합니다.
     * LazyInitializationException 방지 목적입니다.
     */
    @Query("SELECT p FROM Player p JOIN FETCH p.team WHERE p.id IN :ids")
    List<Player> findAllWithTeamByIdIn(@Param("ids") List<Long> ids);
}

