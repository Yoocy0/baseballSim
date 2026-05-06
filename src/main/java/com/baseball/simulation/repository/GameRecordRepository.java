package com.baseball.simulation.repository;

import com.baseball.simulation.entity.GameRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRecordRepository extends JpaRepository<GameRecord, Long> {
}

