package com.baseball.simulation.service.game.logic;

import java.util.Random;
import org.springframework.stereotype.Component;

/**
 * 투구 한 구의 결과를 확률적으로 결정합니다.
 * <p>
 * [확률 흐름 - 일반(NORMAL) 모드]
 * 1. SWING_PROBABILITY(25%) → 스윙 여부
 *    - 스윙 시 SWING_MISS_RATE(60%) → SWING_MISS (스트라이크 카운트)
 *    - 스윙 시 컨택(40%) → FIELD_OUT_ON_CONTACT(70%) → FIELD_OUT
 *                       → 안타(30%) → SINGLE/DOUBLE/TRIPLE/HOMERUN
 * 2. 비스윙 시 CALLED_STRIKE_RATE(70%) → CALLED_STRIKE, 나머지 → CALLED_BALL
 * <p>
 * [CHASE 모드] — 승리 지정 팀이 지고 있을 때 아웃 확률 대폭 감소
 * [LAST_CHANCE 모드] — 기적 로직, 해당 타석 결과를 100% 안타 이상으로 강제
 * <p>
 * TODO(part2): 선수 개인 속성(타율, 장타율, 출루율)으로 확률 상수 대체
 * TODO(part3): 투수 구속/제구/피로도에 따른 STRIKE/BALL 비율 조정
 */
@Component
public class PitchDecider {

    // ── 일반 확률 ──────────────────────────────────────────────────────────────
    private static final double SWING_PROBABILITY     = 0.45;
    private static final double SWING_MISS_RATE       = 0.45;
    private static final double FIELD_OUT_ON_CONTACT  = 0.60;
    private static final double CALLED_STRIKE_RATE    = 0.65;

    // ── 추격(CHASE) 보정 확률 — 아웃 확률 대폭 감소, 안타 확률 대폭 상향 ───────
    private static final double CHASE_SWING_PROB      = 0.60;  // 스윙 확률 25% → 60%
    private static final double CHASE_SWING_MISS      = 0.30;  // 헛스윙률 60% → 25%
    private static final double CHASE_FIELD_OUT       = 0.35;  // 범타률 70% → 25%
    private static final double CHASE_CALLED_STRIKE   = 0.45;  // 낫아웃 70% → 45%

    // ── 안타 종류 누적 확률 (SINGLE 50%, DOUBLE 30%, HOMERUN 15%, TRIPLE 5%) ───
    private static final double SINGLE_THRESHOLD  = 0.50;
    private static final double DOUBLE_THRESHOLD  = 0.80;
    private static final double HOMERUN_THRESHOLD = 0.95;

    private final Random random = new Random();

    /**
     * BattingMode에 따라 투구 결과를 결정합니다.
     *
     * @param mode 현재 타석 판정 모드
     */
    public PitchOutcome decide(BattingMode mode) {
        return switch (mode) {
            case LAST_CHANCE -> forceHit();
            case CHASE       -> decideChase();
            default          -> decideNormal();
        };
    }

    // ── 일반 모드 ──────────────────────────────────────────────────────────────
    private PitchOutcome decideNormal() {
        if (random.nextDouble() < SWING_PROBABILITY) {
            return resolveSwing(SWING_MISS_RATE, FIELD_OUT_ON_CONTACT);
        }
        return random.nextDouble() < CALLED_STRIKE_RATE
                ? PitchOutcome.CALLED_STRIKE
                : PitchOutcome.CALLED_BALL;
    }

    // ── 추격 모드 — 아웃 확률 대폭 감소 ─────────────────────────────────────────
    private PitchOutcome decideChase() {
        if (random.nextDouble() < CHASE_SWING_PROB) {
            return resolveSwing(CHASE_SWING_MISS, CHASE_FIELD_OUT);
        }
        return random.nextDouble() < CHASE_CALLED_STRIKE
                ? PitchOutcome.CALLED_STRIKE
                : PitchOutcome.CALLED_BALL;
    }

    // ── 기적 모드 — 무조건 안타 이상 ────────────────────────────────────────────
    private PitchOutcome forceHit() {
        return resolveHitType();
    }

    private PitchOutcome resolveSwing(double swingMissRate, double fieldOutRate) {
        if (random.nextDouble() < swingMissRate) return PitchOutcome.SWING_MISS;
        if (random.nextDouble() < fieldOutRate)  return PitchOutcome.FIELD_OUT;
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
