package com.baseball.simulation.repository;

import com.baseball.simulation.entity.GameRecord;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GameRecordRepository extends JpaRepository<GameRecord, Long> {

    /**
     * 박스스코어 조회: 특정 경기의 타석 종료(paEnd=true) 레코드를
     * DB 삽입 순서(id ASC = 게임 진행 순서)로 조회합니다.
     */
    @Query("""
            select gr from GameRecord gr
            where gr.game.id = :gameId and gr.paEnd = true
            order by gr.id asc
            """)
    List<GameRecord> findPaEndByGameIdOrderByIdAsc(@Param("gameId") Long gameId);
}
