package com.baseball.simulation.domain.dto;

public record GameRecordDto(
        Long gameId,
        int inning,
        Long pitcherId,
        Long batterId,
        int pitchSequence,
        String pitchResult,
        String paResult,    // 타석 종료 시에만 값이 들어오고, 그 외에는 null
        boolean paEnd,
        String bsoCount
) {
}
