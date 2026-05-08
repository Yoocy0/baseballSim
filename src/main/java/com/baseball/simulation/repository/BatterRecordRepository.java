package com.baseball.simulation.repository;

import com.baseball.simulation.entity.BatterRecord;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

public interface BatterRecordRepository extends JpaRepository<BatterRecord, Long> {

    Optional<BatterRecord> findByBatterIdAndSeasonYear(Long batterId, int seasonYear);

    /** 경기 시작 전 출전 선수들의 현재 시즌 성적을 한 번에 조회합니다. */
    List<BatterRecord> findByBatterIdInAndSeasonYear(List<Long> batterIds, int seasonYear);

    /** 특정 시즌의 전체 BatterRecord를 조회합니다. 정렬은 Service에서 처리합니다. */
    @Query("SELECT br FROM BatterRecord br WHERE br.seasonYear = :seasonYear")
    List<BatterRecord> findBySeasonYear(@Param("seasonYear") int seasonYear);
}
