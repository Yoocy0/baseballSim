package com.baseball.simulation.usecase.game.validator;

import com.baseball.simulation.entity.Game;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GameScheduleValidator {

    /**
     * 현재는 최소 검증만 수행합니다.
     * 이후 파트2/3에서 유효성 검사(정렬, 상태값, 값 범위 등) 로직 확장 예정.
     */
    public void validate(List<Game> games) {
        if (games == null) {
            throw new IllegalStateException("games 조회 결과가 null 입니다.");
        }
    }
}

