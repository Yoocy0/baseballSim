package com.baseball.simulation.service.game.logic;

import java.util.Random;
import org.springframework.stereotype.Component;

/**
 * 투구 한 구의 결과를 확률적으로 결정합니다.
 * <p>
 * [확률 흐름]
 * 1. SWING_PROBABILITY(25%) → 스윙 여부
 *    - 스윙 시 SWING_MISS_RATE(80%) → SWING_MISS (스트라이크 카운트)
 *    - 스윙 시 컨택(20%) → FIELD_OUT_ON_CONTACT(70%) → FIELD_OUT
 *                       → 안타(30%) → SINGLE/DOUBLE/TRIPLE/HOMERUN
 * 2. 비스윙 시 CALLED_STRIKE_RATE(75%) → CALLED_STRIKE, 나머지 → CALLED_BALL
 * <p>
 * TODO(part2): 선수 개인 속성(타율, 장타율, 출루율)으로 확률 상수 대체
 * TODO(part2): 카운트 상황(2스트라이크, 3볼 풀카운트 등)에 따른 스윙 확률 조정
 * TODO(part3): 투수 구속/제구/피로도에 따른 STRIKE/BALL 비율 조정
 */
@Component
public class PitchDecider {

    private static final double SWING_PROBABILITY        = 0.25;
    private static final double SWING_MISS_RATE          = 0.60;
    private static final double FIELD_OUT_ON_CONTACT     = 0.70;
    private static final double CALLED_STRIKE_RATE       = 0.70;

    // 안타 종류 누적 확률 (SINGLE 50%, DOUBLE 30%, HOMERUN 15%, TRIPLE 5%)
    private static final double SINGLE_THRESHOLD  = 0.50;
    private static final double DOUBLE_THRESHOLD  = 0.80;
    private static final double HOMERUN_THRESHOLD = 0.95;

    private final Random random = new Random();

    public PitchOutcome decide() {
        if (random.nextDouble() < SWING_PROBABILITY) {
            return resolveSwing();
        }
        return random.nextDouble() < CALLED_STRIKE_RATE
                ? PitchOutcome.CALLED_STRIKE
                : PitchOutcome.CALLED_BALL;
    }

    private PitchOutcome resolveSwing() {
        // 스윙 → 헛스윙 vs 컨택
        if (random.nextDouble() < SWING_MISS_RATE) {
            return PitchOutcome.SWING_MISS;
        }
        // 컨택 → 범타 vs 안타
        if (random.nextDouble() < FIELD_OUT_ON_CONTACT) {
            return PitchOutcome.FIELD_OUT;
        }
        return resolveHitType();
    }

    private PitchOutcome resolveHitType() {
        double r = random.nextDouble();
        if (r < SINGLE_THRESHOLD)  return PitchOutcome.SINGLE;
        if (r < DOUBLE_THRESHOLD)  return PitchOutcome.DOUBLE;
        if (r < HOMERUN_THRESHOLD) return PitchOutcome.HOMERUN;
        return PitchOutcome.TRIPLE;
    }
}
