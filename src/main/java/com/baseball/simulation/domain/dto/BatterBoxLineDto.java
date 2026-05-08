package com.baseball.simulation.domain.dto;

import java.util.Map;

/**
 * 박스스코어 - 타자 1행 데이터
 * <p>
 * 안타/타점 집계는 제거하고, 이닝별 타석 결과만 표시합니다.
 * (결과 약자: SO=삼진, FO=범타, BB=볼넷, H=단타, 2B=2루타, 3B=3루타, HR=홈런)
 *
 * @param battingOrder  타순 (1~9)
 * @param playerName    선수 이름
 * @param teamName      팀 이름
 * @param inningResults 이닝 번호(1부터) → 결과 약자 (해당 이닝 타석 없으면 키 부재 → "-" 표시)
 */
public record BatterBoxLineDto(
        int battingOrder,
        String playerName,
        String teamName,
        Map<Integer, String> inningResults
) {}
