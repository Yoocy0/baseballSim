package com.baseball.simulation.service.game.logic;

/**
 * 타석 판정 모드를 나타냅니다.
 * <p>
 * - NORMAL     : 일반 확률 (기본)
 * - CHASE      : 추격 보정 — 승리 팀이 지고 있을 때 안타/출루 확률 대폭 상향
 * - LAST_CHANCE: 기적 로직 — 경기 종료 직전(2아웃, 9회 이상) 해당 타석은 100% 안타 이상
 */
public enum BattingMode {
    NORMAL, CHASE, LAST_CHANCE
}
