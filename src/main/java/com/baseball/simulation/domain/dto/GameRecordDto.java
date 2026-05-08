package com.baseball.simulation.domain.dto;

/**
 * 투구 한 구 단위 기록 DTO입니다.
 *
 * @param runsThisPA 이 타석에서 발생한 득점 수.
 *                   paEnd=true 레코드만 0~4 값을 가지며, paEnd=false 레코드는 null입니다.
 */
public record GameRecordDto(
        Long gameId,
        int inning,          // halfInningSeq: 홀수=초, 짝수=말. 실제이닝=(inning+1)/2
        Long pitcherId,
        Long batterId,
        int pitchSequence,   // 해당 타석의 n구째
        String pitchResult,  // 투구 결과 (PitchOutcome.name())
        String paResult,     // 타석 최종 결과 (paEnd=true일 때만 값 존재)
        boolean paEnd,
        String bsoCount,
        Integer runsThisPA   // null=중간투구, 0~4=타석종료시 득점
) {}
