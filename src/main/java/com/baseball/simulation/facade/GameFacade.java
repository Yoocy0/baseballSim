package com.baseball.simulation.facade;

import com.baseball.simulation.domain.BatterStatSnapshot;
import com.baseball.simulation.domain.GameScheduleItem;
import com.baseball.simulation.domain.PitcherStatSnapshot;
import com.baseball.simulation.domain.dto.BatterRankingDto;
import com.baseball.simulation.domain.dto.BoxScoreDto;
import com.baseball.simulation.domain.dto.GameInitDto;
import com.baseball.simulation.domain.dto.GameListItemDto;
import com.baseball.simulation.domain.dto.GameSimulationResultDto;
import com.baseball.simulation.domain.dto.PitcherRankingDto;
import com.baseball.simulation.domain.dto.PlayerDto;
import com.baseball.simulation.domain.dto.TeamRankingDto;
import com.baseball.simulation.service.batterrecord.BatterRecordService;
import com.baseball.simulation.service.game.GameBoxScoreService;
import com.baseball.simulation.service.game.GameDataService;
import com.baseball.simulation.service.game.GameLogicService;
import com.baseball.simulation.service.pitcherrecord.PitcherRecordService;
import com.baseball.simulation.service.teamrecord.TeamRecordService;
import java.time.Year;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 파사드 계층입니다.
 * <p>
 * - Controller의 유일한 의존 대상이며, Service를 직접 주입받을 수 없습니다.
 * - 각 Service는 서로를 직접 주입받지 않으며 파사드가 조합 역할을 합니다.
 * - Repository를 직접 주입받지 않습니다.
 */
@Component
@RequiredArgsConstructor
public class GameFacade {

    private final GameDataService      gameDataService;
    private final GameLogicService     gameLogicService;
    private final TeamRecordService    teamRecordService;
    private final BatterRecordService  batterRecordService;
    private final PitcherRecordService pitcherRecordService;
    private final GameBoxScoreService  gameBoxScoreService;

    /**
     * 랜덤 모드 경기를 진행합니다.
     * <p>
     * [DB 접근 최소화 전략]
     * - 경기 시작 전: 타자·투수 성적을 각 1회 배치 로드
     * - 경기 진행 중: 인메모리에서만 성적 누적 (DB 접근 0회)
     * - 경기 종료 후: 타자·투수 성적 각 1회 saveAll 일괄 저장
     */
    public void startRandomGame() {
        int currentYear = Year.now().getValue();

        GameInitDto initDto = gameDataService.prepareGame();

        // ── 타자 성적 로드 ────────────────────────────────────────────────
        List<Long> allPlayerIds = Stream.concat(
                initDto.teamAPlayers().stream().map(PlayerDto::id),
                initDto.teamBPlayers().stream().map(PlayerDto::id)
        ).toList();
        Map<Long, BatterStatSnapshot> batterStats =
                batterRecordService.loadForGame(allPlayerIds, currentYear);

        // ── 투수 성적 로드 ────────────────────────────────────────────────
        // 현재 구조: 팀의 0번 인덱스 선수가 투수 역할
        // TeamA 수비 시(말 공격 시) 투수 = teamAPlayers[0]
        // TeamB 수비 시(초 공격 시) 투수 = teamBPlayers[0]
        List<Long> pitcherIds = List.of(
                initDto.teamAPlayers().get(0).id(),
                initDto.teamBPlayers().get(0).id()
        );
        Map<Long, PitcherStatSnapshot> pitcherStats =
                pitcherRecordService.loadForGame(pitcherIds, currentYear);

        // ── 시뮬레이션 (메모리 내 성적 업데이트 포함) ─────────────────────
        GameSimulationResultDto resultDto =
                gameLogicService.simulate(initDto, batterStats, pitcherStats);

        // ── 투구 기록 & 경기 결과 DB 저장 ────────────────────────────────
        gameDataService.saveSimulationResult(resultDto);

        // ── 투수 승패·출장 반영 후 일괄 저장 ─────────────────────────────
        Long pitcherAId = initDto.teamAPlayers().get(0).id(); // A팀 투수 (말 공격 때 등판)
        Long pitcherBId = initDto.teamBPlayers().get(0).id(); // B팀 투수 (초 공격 때 등판)
        applyPitcherWinLoss(pitcherStats, pitcherAId, pitcherBId, resultDto);
        pitcherRecordService.batchSave(resultDto.updatedPitcherStats());

        // ── 타자 성적 일괄 저장 ───────────────────────────────────────────
        batterRecordService.batchSave(resultDto.updatedBatterStats());

        // ── 팀 승패 성적 업데이트 ─────────────────────────────────────────
        if (resultDto.finalScoreA() > resultDto.finalScoreB()) {
            teamRecordService.updateAfterGame(
                    initDto.teamA().id(), initDto.teamB().id(), currentYear);
        } else {
            teamRecordService.updateAfterGame(
                    initDto.teamB().id(), initDto.teamA().id(), currentYear);
        }
    }

    /**
     * 최근 경기 기록/일정 목록을 반환합니다.
     */
    public List<GameScheduleItem> getRecentGames() {
        return gameDataService.fetchRecentGames();
    }

    /** 특정 시즌의 팀 순위 목록을 반환합니다. */
    public List<TeamRankingDto> getSeasonRankings(int seasonYear) {
        return teamRecordService.getSeasonRankings(seasonYear);
    }

    /** 특정 시즌의 타자 순위 목록을 반환합니다. */
    public List<BatterRankingDto> getBatterRankings(
            int seasonYear, Comparator<BatterRankingDto> comparator) {
        return batterRecordService.getSeasonRankings(seasonYear, comparator);
    }

    /** 특정 시즌의 투수 순위 목록을 반환합니다. */
    public List<PitcherRankingDto> getPitcherRankings(
            int seasonYear, Comparator<PitcherRankingDto> comparator) {
        return pitcherRecordService.getSeasonRankings(seasonYear, comparator);
    }

    /** 전체 경기 목록을 최신순으로 반환합니다. */
    public List<GameListItemDto> getGameList() {
        return gameBoxScoreService.getGameList();
    }

    /** 특정 경기의 상세 박스스코어를 반환합니다. */
    public BoxScoreDto getBoxScore(Long gameId) {
        return gameBoxScoreService.getBoxScore(gameId);
    }

    // -----------------------------------------------------------------------
    // 내부 헬퍼
    // -----------------------------------------------------------------------

    /**
     * 경기 결과를 바탕으로 투수 성적 스냅샷에 승·패·출장 수를 반영합니다.
     * <p>
     * TeamA가 이기면: TeamA 투수(pitcherAId) → 승, TeamB 투수(pitcherBId) → 패
     * TeamB가 이기면: TeamB 투수(pitcherBId) → 승, TeamA 투수(pitcherAId) → 패
     */
    private void applyPitcherWinLoss(
            Map<Long, PitcherStatSnapshot> pitcherStats,
            Long pitcherAId,
            Long pitcherBId,
            GameSimulationResultDto resultDto
    ) {
        PitcherStatSnapshot snapA = pitcherStats.get(pitcherAId);
        PitcherStatSnapshot snapB = pitcherStats.get(pitcherBId);

        if (snapA != null) snapA.addGame();
        if (snapB != null) snapB.addGame();

        if (resultDto.finalScoreA() > resultDto.finalScoreB()) {
            if (snapA != null) snapA.addWin();
            if (snapB != null) snapB.addLoss();
        } else {
            if (snapB != null) snapB.addWin();
            if (snapA != null) snapA.addLoss();
        }
    }
}
