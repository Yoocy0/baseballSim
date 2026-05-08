package com.baseball.simulation.service.game.logic;

/**
 * 승/패 제어 모드에서 승리 팀이 수비 중일 때 상대 타격 결과를 제한하는 제약 조건입니다.
 * <p>
 * [점수차 기반 허용 득점 상한]
 * - lead >= 4  : 무제한 (만루 홈런을 맞아도 역전 불가, 자유 시뮬레이션)
 * - lead == 3  : maxRunsAllowed = 3  → 그랜드슬램(4점) 차단
 * - lead == 2  : maxRunsAllowed = 2  → 역전 안타 차단, 동점은 허용
 * - lead == 1  : maxRunsAllowed = 1  → 역전 안타 차단, 동점은 허용
 * - lead == 0  : maxRunsAllowed = 0  → 어떤 득점도 불허 (홈런 포함)
 * <p>
 * [끝내기 방지 — walkOffPrevention]
 * 말 공격(isBottom), 9회 이상(lateGame), 0 ≤ lead ≤ 1 조건에서 maxRunsAllowed를 0으로 강제합니다.
 * 이 경우 점수가 날 수 있는 볼넷·안타는 모두 아웃으로 대체됩니다.
 *
 * @param active          수비 제약 활성 여부 (일반 모드 또는 승리 팀이 지고 있는 경우 false)
 * @param maxRunsAllowed  이번 타석에서 허용 가능한 최대 득점 수 (Integer.MAX_VALUE = 무제한)
 */
public record DefensiveConstraint(boolean active, int maxRunsAllowed) {

    /** 제약 없음 (일반 모드 or 4점 차 이상 리드) */
    public static DefensiveConstraint none() {
        return new DefensiveConstraint(false, Integer.MAX_VALUE);
    }

    /**
     * 점수차(lead)와 끝내기 위험도(walkOffRisk)를 기반으로 제약을 계산합니다.
     *
     * @param lead         승리 팀 리드 점수 (양수=리드, 0=동점, 음수=열세)
     * @param walkOffRisk  끝내기 방지 필요 여부 (말 공격 + 9회 이상 + lead ≤ 1)
     */
    public static DefensiveConstraint of(int lead, boolean walkOffRisk) {
        if (lead < 0) return none();           // 승리 팀이 지고 있으면 수비 제약 불필요
        if (lead >= 4) return none();          // 4점 차 이상이면 역전 불가, 제약 없음
        if (walkOffRisk) return cap(0);        // 끝내기 방지: 어떤 득점도 차단
        return cap(lead);                      // 동점 허용 범위까지만 허용
    }

    /**
     * 지정한 득점 수까지만 허용하는 제약을 생성합니다.
     * {@link ScoreModeController}를 포함한 외부 컴포넌트에서도 사용합니다.
     */
    public static DefensiveConstraint cap(int maxRuns) {
        return new DefensiveConstraint(true, maxRuns);
    }

    /** 이 제약이 활성 상태이며 실제로 득점을 제한하는지 여부 */
    public boolean isRestricting() {
        return active && maxRunsAllowed < Integer.MAX_VALUE;
    }
}
