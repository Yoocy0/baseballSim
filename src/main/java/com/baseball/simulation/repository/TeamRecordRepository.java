package com.baseball.simulation.repository;

import com.baseball.simulation.entity.TeamRecord;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeamRecordRepository extends JpaRepository<TeamRecord, Long> {

    Optional<TeamRecord> findByTeamIdAndSeasonYear(Long teamId, int seasonYear);

    /**
     * 특정 시즌의 전체 TeamRecord를 wins 내림차순, 이후 teamId 오름차순으로 조회합니다.
     * winRate 정렬은 Service 레이어에서 추가로 적용합니다.
     * (JPQL은 계산 컬럼 정렬이 제한적이므로 기본 정렬만 DB에서 처리)
     */
    @Query("SELECT tr FROM TeamRecord tr WHERE tr.seasonYear = :seasonYear ORDER BY tr.wins DESC, tr.teamId ASC")
    List<TeamRecord> findBySeasonYearOrderByWinsDesc(@Param("seasonYear") int seasonYear);
}
