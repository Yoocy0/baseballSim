package com.baseball.simulation.service.game;

import com.baseball.simulation.domain.dto.GameInitDto;
import com.baseball.simulation.domain.dto.GameRecordDto;
import com.baseball.simulation.domain.dto.GameSimulationResultDto;
import com.baseball.simulation.domain.dto.PlayerDto;
import com.baseball.simulation.service.game.logic.InningProcessor;
import com.baseball.simulation.service.game.logic.InningProcessor.InningSimResult;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 순수 경기 시뮬레이션 오케스트레이터입니다.
 * <p>
 * Repository 또는 다른 서비스를 주입받지 않으며, DTO를 통해서만 입출력합니다.
 * GameDataService와 직접 의존 관계를 가지지 않습니다.
 * <p>
 * 실제 시뮬레이션 계층 위임 구조:
 *   GameLogicService → InningProcessor → PlateAppearanceProcessor → PitchDecider
 */
@Service
@RequiredArgsConstructor
public class GameLogicService {

    private final InningProcessor inningProcessor;

    /**
     * GameInitDto를 받아 9이닝(+연장) 경기를 메모리에서 시뮬레이션하고
     * 결과(최종 점수 + 전체 투구 기록)를 GameSimulationResultDto로 반환합니다.
     */
    public GameSimulationResultDto simulate(GameInitDto initDto) {
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
            // ── 초 공격: TeamA 공격, TeamB 수비 ──────────────────────────────
            InningSimResult top = inningProcessor.process(
                    initDto.gameId(), inning, true,
                    teamAPlayers, teamBPlayers,
                    battingIndexA,
                    scoreA, scoreB,
                    false, 0, 0,
                    allRecords
            );
            scoreA        += top.runs();
            battingIndexA  = top.nextBattingIndex();

            // 9회 이후, 초 종료 시점에 홈팀(B)이 이미 리드하면 말 공격 불필요
            if (inning >= 9 && scoreB > scoreA) {
                break;
            }

            // ── 말 공격: TeamB 공격, TeamA 수비 ──────────────────────────────
            InningSimResult bottom = inningProcessor.process(
                    initDto.gameId(), inning, false,
                    teamBPlayers, teamAPlayers,
                    battingIndexB,
                    scoreA, scoreB,
                    inning >= 9, scoreA, scoreB,
                    allRecords
            );
            scoreB        += bottom.runs();
            battingIndexB  = bottom.nextBattingIndex();

            // 끝내기로 승부 결정
            if (bottom.walkOff()) {
                break;
            }

            // 연장전: 말 공격 종료 후 점수가 같지 않으면 승부 결정
            if (inning >= 9 && scoreA != scoreB) {
                break;
            }

            inning++;
        }

        System.out.printf("%n[경기 종료] 최종 스코어: %s %d : %d %s%n",
                initDto.teamA().name(), scoreA,
                scoreB, initDto.teamB().name());

        return new GameSimulationResultDto(initDto.gameId(), scoreA, scoreB, allRecords);
    }
}
