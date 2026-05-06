package com.baseball.simulation.repository;

import com.baseball.simulation.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, Long> {
}

