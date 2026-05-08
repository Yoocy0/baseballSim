package com.baseball.simulation.service.game;

import com.baseball.simulation.domain.BatterStatSnapshot;
import com.baseball.simulation.domain.PitcherStatSnapshot;
import com.baseball.simulation.domain.dto.GameInitDto;
import com.baseball.simulation.domain.dto.GameRecordDto;
import com.baseball.simulation.domain.dto.GameSimulationResultDto;
import com.baseball.simulation.domain.dto.PlayerDto;
import com.baseball.simulation.service.game.logic.InningProcessor;
import com.baseball.simulation.service.game.logic.InningProcessor.InningSimResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 순수 경기 시뮬레이션 오케스트레이터입니다.
 * <p>
 * Repository 또는 다른 서비스를 주입받지 않으며, DTO를 통해서만 입출력합니다.
 * GameDataService / BatterRecordService / PitcherRecordService와 직접 의존하지 않습니다.
 * <p>
 * 시뮬레이션 계층:
 *   GameLogicService → InningProcessor → PlateAppearanceProcessor → PitchDecider
 */
@Service
@RequiredArgsConstructor
public class GameLogicService {

    private final InningProcessor inningProcessor;

    /**
     * 경기를 9이닝(+연장) 메모리에서 시뮬레이션합니다.
     *
     * @param initDto      팀·선수·경기 초기화 데이터
     * @param batterStats  경기 시작 전 타자 성적 맵 (경기 중 인메모리 업데이트됨)
     * @param pitcherStats 경기 시작 전 투수 성적 맵 (경기 중 인메모리 업데이트됨)
     * @return 최종 스코어 + 전체 투구 기록 + 최종 타자/투수 성적 스냅샷 목록
     */
    public GameSimulationResultDto simulate(
            GameInitDto initDto,
            Map<Long, BatterStatSnapshot>  batterStats,
            Map<Long, PitcherStatSnapshot> pitcherStats
    ) {
        List<GameRecordDto> allRecords   = new ArrayList<>();
        List<PlayerDto>     teamAPlayers = initDto.teamAPlayers();
        List<PlayerDto>     teamBPlayers = initDto.teamBPlayers();

        int scoreA        = 0;
        int scoreB        = 0;
        int battingIndexA = 0;
        int battingIndexB = 0;
        int inning        = 1;

        System.out.printf("%n[경기 시작] %s vs %s%n",
                initDto.teamA().name(), initDto.teamB().name());

        while (true) {
            // ── 초 공격: TeamA 공격, TeamB 수비 (TeamB 선수[0]이 투수) ────────
            InningSimResult top = inningProcessor.process(
                    initDto.gameId(), inning, true,
                    teamAPlayers, teamBPlayers,
                    battingIndexA,
                    scoreA, scoreB,
                    false, 0, 0,
                    allRecords, batterStats, pitcherStats
            );
            scoreA        += top.runs();
            battingIndexA  = top.nextBattingIndex();

            if (inning >= 9 && scoreB > scoreA) break;

            // ── 말 공격: TeamB 공격, TeamA 수비 (TeamA 선수[0]이 투수) ────────
            InningSimResult bottom = inningProcessor.process(
                    initDto.gameId(), inning, false,
                    teamBPlayers, teamAPlayers,
                    battingIndexB,
                    scoreA, scoreB,
                    inning >= 9, scoreA, scoreB,
                    allRecords, batterStats, pitcherStats
            );
            scoreB        += bottom.runs();
            battingIndexB  = bottom.nextBattingIndex();

            if (bottom.walkOff()) break;

            if (inning >= 9 && scoreA != scoreB) break;

            inning++;
        }

        System.out.printf("%n[경기 종료] 최종 스코어: %s %d : %d %s%n",
                initDto.teamA().name(), scoreA,
                scoreB, initDto.teamB().name());

        return new GameSimulationResultDto(
                initDto.gameId(), scoreA, scoreB,
                allRecords,
                new ArrayList<>(batterStats.values()),
                new ArrayList<>(pitcherStats.values())
        );
    }
}
