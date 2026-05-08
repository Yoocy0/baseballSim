package com.baseball.simulation.service.game.logic;

/**
 * 투구 한 구의 결과를 정의합니다.
 * <p>
 * CALLED_STRIKE: 헛스윙 없이 스트라이크 판정 (비스윙 후 스트라이크 존 통과)
 * CALLED_BALL:   볼 판정
 * SWING_MISS:    헛스윙 (스트라이크 카운트 증가)
 * FIELD_OUT:     컨택 후 범타 – 그라운드 아웃, 플라이 아웃 등 (아웃카운트 즉시 증가)
 * SINGLE / DOUBLE / TRIPLE / HOMERUN: 안타 계열
 * <p>
 * TODO(part2): FOUL(파울), HBP(몸에 맞는 공), SACRIFICE_BUNT(희생번트) 추가
 */
public enum PitchOutcome {
    CALLED_STRIKE,
    CALLED_BALL,
    SWING_MISS,
    FIELD_OUT,
    SINGLE,
    DOUBLE,
    TRIPLE,
    HOMERUN;

    public boolean isStrikeCount() {
        return this == CALLED_STRIKE || this == SWING_MISS;
    }

    public boolean isBallCount() {
        return this == CALLED_BALL;
    }

    /** 타구 아웃 (삼진과 달리 즉시 아웃카운트 증가) */
    public boolean isFieldOut() {
        return this == FIELD_OUT;
    }

    public boolean isHit() {
        return this == SINGLE || this == DOUBLE || this == TRIPLE || this == HOMERUN;
    }

    /** 콘솔 출력용 레이블 */
    public String label() {
        return switch (this) {
            case CALLED_STRIKE -> "CALLED_S";
            case CALLED_BALL   -> "BALL    ";
            case SWING_MISS    -> "SWING_MS";
            case FIELD_OUT     -> "FIELD_OT";
            case SINGLE        -> "SINGLE  ";
            case DOUBLE        -> "DOUBLE  ";
            case TRIPLE        -> "TRIPLE  ";
            case HOMERUN       -> "HOMERUN ";
        };
    }
}
