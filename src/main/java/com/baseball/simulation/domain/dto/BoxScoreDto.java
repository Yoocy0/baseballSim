package com.baseball.simulation.domain.dto;

import java.util.List;

/**
 * 경기 전체 박스스코어 DTO
 *
 * @param gameId           경기 ID
 * @param gameDate         경기 날짜 문자열 (yyyy-MM-dd)
 * @param teamAName        A팀(원정) 이름
 * @param teamBName        B팀(홈) 이름
 * @param inningScoresA    A팀 이닝별 득점 목록 (초 공격 순서)
 * @param inningScoresB    B팀 이닝별 득점 목록 (말 공격 순서, 말이 없으면 마지막 인덱스 부재 → "X")
 * @param totalRunsA       A팀 최종 득점
 * @param totalRunsB       B팀 최종 득점
 * @param totalHitsA       A팀 총 안타
 * @param totalHitsB       B팀 총 안타
 * @param totalWalksA      A팀 총 볼넷
 * @param totalWalksB      B팀 총 볼넷
 * @param teamABatters     A팀 타자 박스스코어 목록 (타순 오름차순)
 * @param teamBBatters     B팀 타자 박스스코어 목록 (타순 오름차순)
 * @param teamAPitcher     A팀 투수 기록
 * @param teamBPitcher     B팀 투수 기록
 */
public record BoxScoreDto(
        Long gameId,
        String gameDate,
        String teamAName,
        String teamBName,
        List<Integer> inningScoresA,
        List<Integer> inningScoresB,
        int totalRunsA,
        int totalRunsB,
        int totalHitsA,
        int totalHitsB,
        int totalWalksA,
        int totalWalksB,
        List<BatterBoxLineDto> teamABatters,
        List<BatterBoxLineDto> teamBBatters,
        PitcherBoxLineDto teamAPitcher,
        PitcherBoxLineDto teamBPitcher
) {}
