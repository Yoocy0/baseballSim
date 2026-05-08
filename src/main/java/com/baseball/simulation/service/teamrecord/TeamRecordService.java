package com.baseball.simulation.service.teamrecord;

import com.baseball.simulation.domain.dto.TeamRankingDto;
import com.baseball.simulation.entity.TeamRecord;
import com.baseball.simulation.usecase.teamrecord.executor.TeamRecordExecutor;
import com.baseball.simulation.usecase.teamrecord.reader.TeamRecordReader;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 팀 시즌 성적 관리를 담당하는 서비스입니다.
 * Repository(usecase 계층)를 주입받으며, 파사드와의 통신은 DTO를 통해서만 합니다.
 * GameDataService / GameLogicService와 직접 의존 관계를 가지지 않습니다.
 */
@Service
@RequiredArgsConstructor
public class TeamRecordService {

    private final TeamRecordReader   teamRecordReader;
    private final TeamRecordExecutor teamRecordExecutor;

    /**
     * 경기 종료 후 승팀/패팀의 시즌 성적을 업데이트합니다.
     * 해당 시즌의 TeamRecord가 없으면 새로 생성합니다.
     *
     * @param winnerTeamId 승리 팀 ID
     * @param loserTeamId  패배 팀 ID
     * @param seasonYear   시즌 연도
     */
    @Transactional
    public void updateAfterGame(Long winnerTeamId, Long loserTeamId, int seasonYear) {
        TeamRecord winnerRecord = findOrCreate(winnerTeamId, seasonYear);
        winnerRecord.addWin();
        teamRecordExecutor.save(winnerRecord);

        TeamRecord loserRecord = findOrCreate(loserTeamId, seasonYear);
        loserRecord.addLoss();
        teamRecordExecutor.save(loserRecord);
    }

    /**
     * 특정 시즌의 팀 순위 목록을 반환합니다.
     * 정렬 기준: wins 내림차순 → winRate 내림차순 → teamName 오름차순
     *
     * @param seasonYear 시즌 연도
     * @return 순위 정렬된 TeamRankingDto 목록
     */
    @Transactional(readOnly = true)
    public List<TeamRankingDto> getSeasonRankings(int seasonYear) {
        List<TeamRecord> records = teamRecordReader.readSeasonRecords(seasonYear);
        if (records.isEmpty()) {
            return List.of();
        }

        // teamId 목록을 모아 팀명을 한 번에 조회 → Map<teamId, teamName>
        List<Long> teamIds = records.stream()
                .map(TeamRecord::getTeamId)
                .collect(Collectors.toList());
        Map<Long, String> teamNameMap = teamRecordReader.readTeamNameMap(teamIds);

        return records.stream()
                .sorted(Comparator
                        .comparingInt(TeamRecord::getWins)
                        .thenComparingDouble(TeamRecord::winRate).reversed()
                        .thenComparing(r -> teamNameMap.getOrDefault(r.getTeamId(), "")))
                .map(r -> new TeamRankingDto(
                        teamNameMap.getOrDefault(r.getTeamId(), "팀 ID " + r.getTeamId()),
                        r.getWins(),
                        r.getLosses(),
                        r.winRate()
                ))
                .collect(Collectors.toList());
    }

    private TeamRecord findOrCreate(Long teamId, int seasonYear) {
        return teamRecordReader.findRecord(teamId, seasonYear)
                .orElseGet(() -> teamRecordExecutor.create(teamId, seasonYear));
    }
}
