package com.baseball.simulation.usecase.pitcherrecord.reader;

import com.baseball.simulation.entity.PitcherRecord;
import com.baseball.simulation.entity.Player;
import com.baseball.simulation.repository.PitcherRecordRepository;
import com.baseball.simulation.repository.PlayerRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * PitcherRecord 조회 전용 Usecase 컴포넌트입니다.
 */
@Component
@RequiredArgsConstructor
public class PitcherRecordReader {

    private final PitcherRecordRepository pitcherRecordRepository;
    private final PlayerRepository        playerRepository;

    public Optional<PitcherRecord> findRecord(Long pitcherId, int seasonYear) {
        return pitcherRecordRepository.findByPitcherIdAndSeasonYear(pitcherId, seasonYear);
    }

    public List<PitcherRecord> findRecordsForGame(List<Long> pitcherIds, int seasonYear) {
        return pitcherRecordRepository.findByPitcherIdInAndSeasonYear(pitcherIds, seasonYear);
    }

    public List<PitcherRecord> readSeasonRecords(int seasonYear) {
        return pitcherRecordRepository.findBySeasonYear(seasonYear);
    }

    /**
     * JOIN FETCH로 Player + Team을 한 번에 조회해 Map 형태로 반환합니다.
     */
    public PlayerInfoMaps loadPlayerInfoMaps(List<Long> pitcherIds) {
        List<Player> players = playerRepository.findAllWithTeamByIdIn(pitcherIds);
        Map<Long, String> nameMap = players.stream()
                .collect(Collectors.toMap(Player::getId, Player::getName));
        Map<Long, String> teamNameMap = players.stream()
                .collect(Collectors.toMap(
                        Player::getId,
                        p -> p.getTeam() != null ? p.getTeam().getName() : "알 수 없음"
                ));
        return new PlayerInfoMaps(nameMap, teamNameMap);
    }

    public record PlayerInfoMaps(
            Map<Long, String> playerNameMap,
            Map<Long, String> teamNameMap
    ) {}
}
