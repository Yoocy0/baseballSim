package com.baseball.simulation.domain.dto;

import java.util.List;

public record GameSimulationResultDto(
        Long gameId,
        int finalScoreA,
        int finalScoreB,
        List<GameRecordDto> records
) {
}
