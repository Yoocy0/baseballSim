package com.baseball.simulation.service.game;

import com.baseball.simulation.domain.BatterStatSnapshot;
import com.baseball.simulation.domain.PitcherStatSnapshot;
import com.baseball.simulation.domain.ScoreTargetContext;
import com.baseball.simulation.domain.WinControlContext;
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
 * <p>
 * 시뮬레이션 계층:
 *   GameLogicService → InningProcessor → PlateAppearanceProcessor → PitchDecider
 * <p>
 * [경기 종료 조건]
 * <ul>
 *   <li>일반/승패 모드: 9이닝 이후 승패 결정 시 (또는 끝내기)</li>
 *   <li>스코어 모드: 양 팀 모두 타겟 점수 달성 시 (최소 9이닝 보장)</li>
 * </ul>
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
     * @param winCtx       승/패 제어 모드 컨텍스트 (일반 모드이면 WinControlContext.normal())
     * @param scoreCtx     스코어 모드 컨텍스트 (비활성이면 ScoreTargetContext.none())
     * @return 최종 스코어 + 전체 투구 기록 + 최종 타자/투수 성적 스냅샷 목록
     */
    public GameSimulationResultDto simulate(
            GameInitDto initDto,
            Map<Long, BatterStatSnapshot>  batterStats,
            Map<Long, PitcherStatSnapshot> pitcherStats,
            WinControlContext winCtx,
            ScoreTargetContext scoreCtx
    ) {
        List<GameRecordDto> allRecords   = new ArrayList<>();
        List<PlayerDto>     teamAPlayers = initDto.teamAPlayers();
        List<PlayerDto>     teamBPlayers = initDto.teamBPlayers();

        int scoreA        = 0;
        int scoreB        = 0;
        int battingIndexA = 0;
        int battingIndexB = 0;
        int inning        = 1;

        List<Integer> inningScoresA = new ArrayList<>();
        List<Integer> inningScoresB = new ArrayList<>();

        System.out.printf("%n[경기 시작] %s vs %s%n",
                initDto.teamA().name(), initDto.teamB().name());
        if (scoreCtx.isScoreMode()) {
            System.out.printf("[스코어 모드] 타겟: %s %d점 / %s %d점%n",
                    initDto.teamA().name(), scoreCtx.targetScoreA(),
                    initDto.teamB().name(), scoreCtx.targetScoreB());
        }

        boolean extendedLogPrinted = false;

        while (true) {
            // ── 초 공격: A팀 공격 ────────────────────────────────────────────
            InningSimResult top = inningProcessor.process(
                    initDto.gameId(), inning, true,
                    teamAPlayers, teamBPlayers,
                    battingIndexA,
                    scoreA, scoreB,
                    false, 0, 0,
                    allRecords, batterStats, pitcherStats,
                    winCtx, scoreCtx
            );
            scoreA        += top.runs();
            battingIndexA  = top.nextBattingIndex();
            inningScoresA.add(top.runs());

            // ── 초 공격 후 종료 판정 ─────────────────────────────────────────
            if (scoreCtx.isScoreMode()) {
                if (inning >= 9 && scoreCtx.bothDone(scoreA, scoreB)) break;
                // 스코어 모드 연장 진입 로그 (1회만 출력)
                if (inning >= 9 && !scoreCtx.bothDone(scoreA, scoreB) && !extendedLogPrinted) {
                    System.out.println("[스코어 조작] 타겟 점수 달성을 위해 연장전 진입 및 확률 보정을 가동합니다.");
                    extendedLogPrinted = true;
                }
            } else {
                // 일반/승패 모드: 9회 이후 홈팀이 앞서면 말 공격 없이 종료
                if (inning >= 9 && scoreB > scoreA) break;
            }

            // ── 말 공격: B팀 공격 ────────────────────────────────────────────
            InningSimResult bottom = inningProcessor.process(
                    initDto.gameId(), inning, false,
                    teamBPlayers, teamAPlayers,
                    battingIndexB,
                    scoreA, scoreB,
                    inning >= 9 && !scoreCtx.isScoreMode(), scoreA, scoreB,
                    allRecords, batterStats, pitcherStats,
                    winCtx, scoreCtx
            );
            scoreB        += bottom.runs();
            battingIndexB  = bottom.nextBattingIndex();
            inningScoresB.add(bottom.runs());

            // ── 말 공격 후 종료 판정 ─────────────────────────────────────────
            if (scoreCtx.isScoreMode()) {
                if (inning >= 9 && scoreCtx.bothDone(scoreA, scoreB)) break;
                if (inning >= 9 && !scoreCtx.bothDone(scoreA, scoreB) && !extendedLogPrinted) {
                    System.out.println("[스코어 조작] 타겟 점수 달성을 위해 연장전 진입 및 확률 보정을 가동합니다.");
                    extendedLogPrinted = true;
                }
            } else {
                if (bottom.walkOff()) break;
                if (inning >= 9 && scoreA != scoreB) break;
            }

            inning++;
        }

        System.out.printf("%n[경기 종료] 최종 스코어: %s %d : %d %s%n",
                initDto.teamA().name(), scoreA,
                scoreB, initDto.teamB().name());

        return new GameSimulationResultDto(
                initDto.gameId(), scoreA, scoreB,
                allRecords,
                new ArrayList<>(batterStats.values()),
                new ArrayList<>(pitcherStats.values()),
                inningScoresA,
                inningScoresB
        );
    }
}
