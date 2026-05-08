package com.baseball.simulation.repository;

import com.baseball.simulation.entity.PitcherRecord;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PitcherRecordRepository extends JpaRepository<PitcherRecord, Long> {

    Optional<PitcherRecord> findByPitcherIdAndSeasonYear(Long pitcherId, int seasonYear);

    /** 경기 시작 전 출전 투수들의 현재 시즌 성적을 한 번에 조회합니다. */
    List<PitcherRecord> findByPitcherIdInAndSeasonYear(List<Long> pitcherIds, int seasonYear);

    /** 특정 시즌의 전체 PitcherRecord를 조회합니다. 정렬은 Service에서 처리합니다. */
    @Query("SELECT pr FROM PitcherRecord pr WHERE pr.seasonYear = :seasonYear")
    List<PitcherRecord> findBySeasonYear(@Param("seasonYear") int seasonYear);
}
