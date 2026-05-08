package com.baseball.simulation.usecase.game.validator;

import com.baseball.simulation.entity.Team;
import org.springframework.stereotype.Component;

@Component
public class GameSimulationValidator {

    /**
     * 현재는 최소 검증만 수행합니다.
     * 이후 파트2/3에서 팀/선수/상태 값 유효성 검증을 확장 예정입니다.
     */
    public void validateTeams(Team teamA, Team teamB) {
        if (teamA == null || teamB == null) {
            throw new IllegalStateException("시뮬레이션에 필요한 팀 데이터가 존재하지 않습니다.");
        }
    }
}

