package com.baseball.simulation.domain.dto;

import java.util.List;

public record GameInitDto(
        Long gameId,
        TeamDto teamA,
        TeamDto teamB,
        List<PlayerDto> teamAPlayers,
        List<PlayerDto> teamBPlayers
) {
}
