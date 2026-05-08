package com.baseball.simulation.service.pitcherrecord;

import com.baseball.simulation.domain.PitcherStatSnapshot;
import com.baseball.simulation.domain.dto.PitcherRankingDto;
import com.baseball.simulation.entity.PitcherRecord;
import com.baseball.simulation.usecase.pitcherrecord.executor.PitcherRecordExecutor;
import com.baseball.simulation.usecase.pitcherrecord.reader.PitcherRecordReader;
import com.baseball.simulation.usecase.pitcherrecord.reader.PitcherRecordReader.PlayerInfoMaps;
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
 * 투수 시즌 성적 관리를 담당하는 서비스입니다.
 * <p>
 * - 경기 시작 전: DB에서 성적을 한 번 로드해 인메모리 Map으로 반환
 * - 경기 종료 후: 메모리의 최종 성적을 saveAll로 일괄 DB 반영
 * - GameDataService / GameLogicService와 직접 의존 관계를 가지지 않습니다.
 */
@Service
@RequiredArgsConstructor
public class PitcherRecordService {

    private final PitcherRecordReader   pitcherRecordReader;
    private final PitcherRecordExecutor pitcherRecordExecutor;

    /**
     * 경기 시작 전 출전 투수들의 현재 시즌 성적을 DB에서 한 번 로드합니다.
     * DB에 성적이 없는 투수는 빈 스냅샷(0 기준)으로 초기화합니다.
     */
    @Transactional(readOnly = true)
    public Map<Long, PitcherStatSnapshot> loadForGame(List<Long> pitcherIds, int seasonYear) {
        List<PitcherRecord> existing =
                pitcherRecordReader.findRecordsForGame(pitcherIds, seasonYear);

        Map<Long, PitcherStatSnapshot> snapshotMap = new HashMap<>();
        for (PitcherRecord record : existing) {
            snapshotMap.put(record.getPitcherId(), PitcherStatSnapshot.from(record));
        }
        for (Long pitcherId : pitcherIds) {
            snapshotMap.computeIfAbsent(pitcherId,
                    id -> PitcherStatSnapshot.blank(id, seasonYear));
        }
        return snapshotMap;
    }

    /**
     * 경기 종료 후 인메모리 스냅샷 목록을 PitcherRecord 엔티티로 변환해 일괄 저장합니다.
     *
     * @param snapshots GameLogicService가 반환한 최종 투수 성적 스냅샷 목록
     */
    @Transactional
    public void batchSave(List<PitcherStatSnapshot> snapshots) {
        List<PitcherRecord> toSave = new ArrayList<>();
        for (PitcherStatSnapshot snap : snapshots) {
            PitcherRecord record = resolveRecord(snap);
            applySnapshot(record, snap);
            toSave.add(record);
        }
        pitcherRecordExecutor.saveAll(toSave);
    }

    /**
     * 특정 시즌의 투수 순위 목록을 반환합니다.
     *
     * @param seasonYear 시즌 연도
     * @param comparator 정렬 기준
     */
    @Transactional(readOnly = true)
    public List<PitcherRankingDto> getSeasonRankings(
            int seasonYear,
            Comparator<PitcherRankingDto> comparator
    ) {
        List<PitcherRecord> records = pitcherRecordReader.readSeasonRecords(seasonYear);
        if (records.isEmpty()) return List.of();

        List<Long> pitcherIds = records.stream()
                .map(PitcherRecord::getPitcherId)
                .collect(Collectors.toList());
        PlayerInfoMaps infoMaps = pitcherRecordReader.loadPlayerInfoMaps(pitcherIds);

        return records.stream()
                .map(r -> toRankingDto(r, infoMaps))
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    // -----------------------------------------------------------------------
    // 내부 헬퍼
    // -----------------------------------------------------------------------

    private PitcherRecord resolveRecord(PitcherStatSnapshot snap) {
        return pitcherRecordReader.findRecord(snap.getPitcherId(), snap.getSeasonYear())
                .orElseGet(() -> PitcherRecord.create(snap.getPitcherId(), snap.getSeasonYear()));
    }

    private void applySnapshot(PitcherRecord record, PitcherStatSnapshot snap) {
        record.setGames(snap.getGames());
        record.setWins(snap.getWins());
        record.setLosses(snap.getLosses());
        record.setStrikeouts(snap.getStrikeouts());
        record.setWalks(snap.getWalks());
        record.setInningsX3(snap.getInningsX3());
        record.setHitsAllowed(snap.getHitsAllowed());
        record.setHomeRunsAllowed(snap.getHomeRunsAllowed());
        record.setBattersFaced(snap.getBattersFaced());
        record.setPitchesThrown(snap.getPitchesThrown());
        record.setRunsAllowed(snap.getRunsAllowed());
    }

    private PitcherRankingDto toRankingDto(PitcherRecord r, PlayerInfoMaps infoMaps) {
        String playerName = infoMaps.playerNameMap().getOrDefault(
                r.getPitcherId(), "선수 ID " + r.getPitcherId());
        String teamName   = infoMaps.teamNameMap().getOrDefault(r.getPitcherId(), "미정");

        // ERA = (runsAllowed * 9) / (inningsX3 / 3.0) — 분모 0 방어
        double era = r.getInningsX3() == 0 ? 0.0
                : (r.getRunsAllowed() * 9.0) / (r.getInningsX3() / 3.0);

        // 피안타율 = hitsAllowed / (battersFaced - walks) — 분모 0 방어
        int    officialAB  = r.getBattersFaced() - r.getWalks();
        double avgAllowed  = officialAB == 0 ? 0.0
                : (double) r.getHitsAllowed() / officialAB;

        // WHIP = (hitsAllowed + walks) / (inningsX3 / 3.0) — 분모 0 방어
        double whip = r.getInningsX3() == 0 ? 0.0
                : (r.getHitsAllowed() + r.getWalks()) / (r.getInningsX3() / 3.0);

        return new PitcherRankingDto(
                playerName, teamName,
                r.getGames(), r.getWins(), r.getLosses(),
                r.getStrikeouts(), r.getWalks(), r.getInningsX3(),
                r.getHitsAllowed(), r.getHomeRunsAllowed(),
                r.getBattersFaced(), r.getPitchesThrown(), r.getRunsAllowed(),
                era, avgAllowed, whip
        );
    }
}
