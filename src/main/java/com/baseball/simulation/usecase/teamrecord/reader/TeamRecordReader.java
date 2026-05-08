package com.baseball.simulation.usecase.teamrecord.reader;

import com.baseball.simulation.entity.Team;
import com.baseball.simulation.entity.TeamRecord;
import com.baseball.simulation.repository.TeamRecordRepository;
import com.baseball.simulation.repository.TeamRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * TeamRecord 조회 전용 Usecase 컴포넌트입니다.
 */
@Component
@RequiredArgsConstructor
public class TeamRecordReader {

    private final TeamRecordRepository teamRecordRepository;
    private final TeamRepository       teamRepository;

    /** 특정 팀의 특정 시즌 성적을 조회합니다. 없으면 Optional.empty() */
    public Optional<TeamRecord> findRecord(Long teamId, int seasonYear) {
        return teamRecordRepository.findByTeamIdAndSeasonYear(teamId, seasonYear);
    }

    /** 특정 시즌의 모든 TeamRecord를 wins 내림차순으로 조회합니다. */
    public List<TeamRecord> readSeasonRecords(int seasonYear) {
        return teamRecordRepository.findBySeasonYearOrderByWinsDesc(seasonYear);
    }

    /**
     * 주어진 팀 ID 목록에 해당하는 Team 엔티티를 Map(id → name)으로 반환합니다.
     * 순위 출력 시 팀명 매핑에 사용합니다.
     */
    public Map<Long, String> readTeamNameMap(List<Long> teamIds) {
        return teamRepository.findAllById(teamIds).stream()
                .collect(Collectors.toMap(Team::getId, Team::getName));
    }
}
