package com.baseball.simulation.service.batterrecord;

import com.baseball.simulation.domain.BatterStatSnapshot;
import com.baseball.simulation.domain.dto.BatterRankingDto;
import com.baseball.simulation.entity.BatterRecord;
import com.baseball.simulation.usecase.batterrecord.executor.BatterRecordExecutor;
import com.baseball.simulation.usecase.batterrecord.reader.BatterRecordReader;
import com.baseball.simulation.usecase.batterrecord.reader.BatterRecordReader.PlayerInfoMaps;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 타자 시즌 성적 관리를 담당하는 서비스입니다.
 * <p>
 * - 경기 시작 전: DB에서 성적을 한 번 로드해 인메모리 Map으로 반환
 * - 경기 종료 후: 메모리의 최종 성적을 saveAll로 일괄 DB 반영
 * - GameDataService / GameLogicService와 직접 의존 관계를 가지지 않습니다.
 */
@Service
@RequiredArgsConstructor
public class BatterRecordService {

    private final BatterRecordReader   batterRecordReader;
    private final BatterRecordExecutor batterRecordExecutor;

    /**
     * 경기 시작 전 출전 선수들의 현재 시즌 성적을 DB에서 한 번 로드해
     * Map(batterId → BatterStatSnapshot)으로 반환합니다.
     * DB에 성적이 없는 선수는 빈 스냅샷(0 기준)으로 초기화합니다.
     *
     * @param allPlayerIds teamA + teamB 전체 출전 선수 ID 목록
     * @param seasonYear   현재 시즌 연도
     * @return 인메모리 성적 맵 (GameLogicService에 전달)
     */
    @Transactional(readOnly = true)
    public Map<Long, BatterStatSnapshot> loadForGame(List<Long> allPlayerIds, int seasonYear) {
        List<BatterRecord> existing = batterRecordReader.findRecordsForGame(allPlayerIds, seasonYear);

        Map<Long, BatterStatSnapshot> snapshotMap = new HashMap<>();
        for (BatterRecord record : existing) {
            snapshotMap.put(record.getBatterId(), BatterStatSnapshot.from(record));
        }

        // DB에 성적 없는 선수 → 빈 스냅샷으로 초기화
        for (Long playerId : allPlayerIds) {
            snapshotMap.computeIfAbsent(playerId, id -> BatterStatSnapshot.blank(id, seasonYear));
        }

        return snapshotMap;
    }

    /**
     * 경기 종료 후 인메모리 스냅샷 목록을 BatterRecord 엔티티로 변환해 일괄 저장합니다.
     *
     * @param snapshots GameLogicService가 반환한 최종 성적 스냅샷 목록
     */
    @Transactional
    public void batchSave(List<BatterStatSnapshot> snapshots) {
        List<BatterRecord> toSave = new ArrayList<>();
        for (BatterStatSnapshot snap : snapshots) {
            BatterRecord record = resolveRecord(snap);
            applySnapshot(record, snap);
            toSave.add(record);
        }
        batterRecordExecutor.saveAll(toSave);
    }

    /**
     * 특정 시즌의 타자 순위 목록을 반환합니다.
     *
     * @param seasonYear 시즌 연도
     * @param comparator 정렬 기준 (BatterRankingDto 필드 기준)
     */
    @Transactional(readOnly = true)
    public List<BatterRankingDto> getSeasonRankings(
            int seasonYear,
            Comparator<BatterRankingDto> comparator
    ) {
        List<BatterRecord> records = batterRecordReader.readSeasonRecords(seasonYear);
        if (records.isEmpty()) return List.of();

        List<Long> batterIds = records.stream()
                .map(BatterRecord::getBatterId)
                .collect(Collectors.toList());
        PlayerInfoMaps infoMaps = batterRecordReader.loadPlayerInfoMaps(batterIds);

        return records.stream()
                .map(r -> toRankingDto(r, infoMaps))
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    // -----------------------------------------------------------------------
    // 내부 헬퍼
    // -----------------------------------------------------------------------

    private BatterRecord resolveRecord(BatterStatSnapshot snap) {
        if (snap.getId() != null) {
            return batterRecordReader.findRecord(snap.getBatterId(), snap.getSeasonYear())
                    .orElseGet(() -> batterRecordExecutor.create(snap.getBatterId(), snap.getSeasonYear()));
        }
        return batterRecordReader.findRecord(snap.getBatterId(), snap.getSeasonYear())
                .orElseGet(() -> BatterRecord.create(snap.getBatterId(), snap.getSeasonYear()));
    }

    private void applySnapshot(BatterRecord record, BatterStatSnapshot snap) {
        record.setPlateAppearances(snap.getPlateAppearances());
        record.setAtBats(snap.getAtBats());
        record.setHits(snap.getHits());
        record.setDoubles(snap.getDoubles());
        record.setTriples(snap.getTriples());
        record.setHomeRuns(snap.getHomeRuns());
        record.setRuns(snap.getRuns());
        record.setRbi(snap.getRbi());
        record.setStrikeouts(snap.getStrikeouts());
        record.setWalks(snap.getWalks());
    }

    private BatterRankingDto toRankingDto(BatterRecord r, PlayerInfoMaps infoMaps) {
        String playerName = infoMaps.playerNameMap().getOrDefault(r.getBatterId(), "선수 ID " + r.getBatterId());
        String teamName   = infoMaps.teamNameMap().getOrDefault(r.getBatterId(), "미정");

        int    singles   = r.getHits() - r.getDoubles() - r.getTriples() - r.getHomeRuns();
        double avg       = r.getAtBats()           == 0 ? 0.0 : (double) r.getHits() / r.getAtBats();
        double obp       = r.getPlateAppearances() == 0 ? 0.0 : (double) (r.getHits() + r.getWalks()) / r.getPlateAppearances();
        double slg       = r.getAtBats()           == 0 ? 0.0
                : (double) (singles + 2 * r.getDoubles() + 3 * r.getTriples() + 4 * r.getHomeRuns()) / r.getAtBats();

        return new BatterRankingDto(
                playerName, teamName,
                r.getPlateAppearances(), r.getAtBats(),
                r.getHits(), r.getDoubles(), r.getTriples(), r.getHomeRuns(),
                r.getRuns(), r.getRbi(), r.getStrikeouts(), r.getWalks(),
                avg, obp, slg, obp + slg
        );
    }
}
