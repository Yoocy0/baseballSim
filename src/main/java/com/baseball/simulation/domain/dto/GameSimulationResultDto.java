package com.baseball.simulation.domain.dto;

import com.baseball.simulation.domain.BatterStatSnapshot;
import com.baseball.simulation.domain.PitcherStatSnapshot;
import java.util.List;

/**
 * 경기 시뮬레이션 최종 결과 DTO입니다.
 *
 * @param gameId               경기 ID
 * @param finalScoreA          A팀 최종 점수
 * @param finalScoreB          B팀 최종 점수
 * @param records              전체 투구 기록 (DB 일괄 저장용)
 * @param updatedBatterStats   경기 종료 후 최종 타자 성적 스냅샷 목록
 * @param updatedPitcherStats  경기 종료 후 최종 투수 성적 스냅샷 목록
 * @param inningScoresA        이닝별 A팀 득점 목록 (초 공격 순서)
 * @param inningScoresB        이닝별 B팀 득점 목록 (말 공격 순서, 마지막 이닝이 없으면 "X" 처리)
 */
public record GameSimulationResultDto(
        Long gameId,
        int finalScoreA,
        int finalScoreB,
        List<GameRecordDto> records,
        List<BatterStatSnapshot> updatedBatterStats,
        List<PitcherStatSnapshot> updatedPitcherStats,
        List<Integer> inningScoresA,
        List<Integer> inningScoresB
) {}
