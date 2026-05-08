package com.baseball.simulation.facade;

import com.baseball.simulation.domain.GameScheduleItem;
import com.baseball.simulation.domain.dto.GameInitDto;
import com.baseball.simulation.domain.dto.GameSimulationResultDto;
import com.baseball.simulation.service.game.GameDataService;
import com.baseball.simulation.service.game.GameLogicService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 파사드 계층입니다.
 * - Controller의 유일한 의존 대상이며, Service를 직접 주입받을 수 없습니다.
 * - GameDataService(DB 담당)와 GameLogicService(로직 담당)를 조합하고,
 *   두 서비스 간 데이터는 DTO를 통해서만 전달합니다.
 * - Repository를 직접 주입받지 않습니다.
 *
 * 멀티모듈 전환 시 파사드가 모듈 경계 역할을 담당할 수 있도록
 * 서비스 간 직접 의존 없이 설계되었습니다.
 */
@Component
@RequiredArgsConstructor
public class GameFacade {

    private final GameDataService gameDataService;
    private final GameLogicService gameLogicService;

    /**
     * 랜덤 모드 경기를 진행합니다.
     * 1. DataService에서 팀/선수/게임 초기 데이터를 DTO로 조회
     * 2. LogicService에서 DTO를 받아 시뮬레이션을 메모리에서 실행, 결과 DTO 반환
     * 3. DataService에 결과 DTO를 전달해 DB에 저장
     */
    public void startRandomGame() {
        GameInitDto initDto = gameDataService.prepareGame();
        GameSimulationResultDto resultDto = gameLogicService.simulate(initDto);
        gameDataService.saveSimulationResult(resultDto);

        System.out.printf("최종 스코어: %s %d : %d %s%n",
                initDto.teamA().name(),
                resultDto.finalScoreA(),
                resultDto.finalScoreB(),
                initDto.teamB().name());
    }

    /**
     * 최근 경기 기록/일정 목록을 반환합니다.
     * TODO(part2): 날짜별 일정 조회로 확장
     */
    public List<GameScheduleItem> getRecentGames() {
        return gameDataService.fetchRecentGames();
    }
}
