package com.baseball.simulation.usecase.batterrecord.reader;

import com.baseball.simulation.entity.BatterRecord;
import com.baseball.simulation.entity.Player;
import com.baseball.simulation.repository.BatterRecordRepository;
import com.baseball.simulation.repository.PlayerRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * BatterRecord 조회 전용 Usecase 컴포넌트입니다.
 */
@Component
@RequiredArgsConstructor
public class BatterRecordReader {

    private final BatterRecordRepository batterRecordRepository;
    private final PlayerRepository       playerRepository;

    /** 특정 타자의 특정 시즌 성적을 조회합니다. */
    public Optional<BatterRecord> findRecord(Long batterId, int seasonYear) {
        return batterRecordRepository.findByBatterIdAndSeasonYear(batterId, seasonYear);
    }

    /**
     * 경기 출전 타자 목록의 현재 시즌 성적을 한 번에 조회합니다.
     * 없는 선수는 결과에 포함되지 않으므로 호출자가 blank 스냅샷을 생성해야 합니다.
     */
    public List<BatterRecord> findRecordsForGame(List<Long> batterIds, int seasonYear) {
        return batterRecordRepository.findByBatterIdInAndSeasonYear(batterIds, seasonYear);
    }

    /** 특정 시즌의 전체 BatterRecord를 조회합니다. */
    public List<BatterRecord> readSeasonRecords(int seasonYear) {
        return batterRecordRepository.findBySeasonYear(seasonYear);
    }

    /**
     * 주어진 선수 ID 목록의 Player 엔티티를 Team과 함께 Fetch Join으로 조회해
     * Map(batterId → playerName), Map(batterId → teamName)을 반환합니다.
     * LazyInitializationException 방지 목적입니다.
     */
    public PlayerInfoMaps loadPlayerInfoMaps(List<Long> batterIds) {
        List<Player> players = playerRepository.findAllWithTeamByIdIn(batterIds);
        Map<Long, String> nameMap     = players.stream()
                .collect(Collectors.toMap(Player::getId, Player::getName));
        Map<Long, String> teamNameMap = players.stream()
                .collect(Collectors.toMap(
                        Player::getId,
                        p -> p.getTeam() != null ? p.getTeam().getName() : "알 수 없음"
                ));
        return new PlayerInfoMaps(nameMap, teamNameMap);
    }

    /** 선수명·팀명 Map 쌍을 묶어 반환하는 값 객체 */
    public record PlayerInfoMaps(
            Map<Long, String> playerNameMap,
            Map<Long, String> teamNameMap
    ) {}
}
