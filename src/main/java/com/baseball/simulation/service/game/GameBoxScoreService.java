package com.baseball.simulation.service.game;

import com.baseball.simulation.domain.dto.BatterBoxLineDto;
import com.baseball.simulation.domain.dto.BoxScoreDto;
import com.baseball.simulation.domain.dto.GameListItemDto;
import com.baseball.simulation.domain.dto.PitcherBoxLineDto;
import com.baseball.simulation.entity.Game;
import com.baseball.simulation.entity.GameRecord;
import com.baseball.simulation.entity.Player;
import com.baseball.simulation.repository.PlayerRepository;
import com.baseball.simulation.usecase.game.reader.GameBoxScoreReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 경기 목록 조회 및 박스스코어 조립 서비스입니다.
 * <p>
 * 이닝 탐색 알고리즘: PA 레코드의 halfInningSeq로부터 실제 이닝 번호(1~N)를 역산한 후
 * 1부터 순서대로 순회하여 연장전을 포함한 모든 이닝을 처리합니다.
 * 팀 구분은 player.team.id 기준으로 수행합니다.
 */
@Service
@RequiredArgsConstructor
public class GameBoxScoreService {

    private final GameBoxScoreReader boxScoreReader;
    private final PlayerRepository playerRepository;

    // ─────────────────────────────────────────────────────────────────
    // 경기 목록
    // ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<GameListItemDto> getGameList() {
        List<Game> games = boxScoreReader.findAllGamesWithTeams();
        List<GameListItemDto> result = new ArrayList<>();
        for (int i = 0; i < games.size(); i++) {
            Game g = games.get(i);
            result.add(new GameListItemDto(
                    g.getId(),
                    g.getGameDate() != null ? g.getGameDate().toString() : "-",
                    g.getTeamA() != null ? g.getTeamA().getName() : "팀A",
                    g.getTeamB() != null ? g.getTeamB().getName() : "팀B",
                    g.getScoreA(),
                    g.getScoreB(),
                    g.getStatus()
            ));
        }
        return result;
    }

    // ─────────────────────────────────────────────────────────────────
    // 박스스코어 조립
    // ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public BoxScoreDto getBoxScore(Long gameId) {
        Game game = boxScoreReader.findGameWithTeams(gameId);
        String teamAName = game.getTeamA() != null ? game.getTeamA().getName() : "팀A";
        String teamBName = game.getTeamB() != null ? game.getTeamB().getName() : "팀B";
        Long   teamAId   = game.getTeamA() != null ? game.getTeamA().getId()   : null;
        Long   teamBId   = game.getTeamB() != null ? game.getTeamB().getId()   : null;

        // PA 종료 레코드를 경기 진행 순서(id ASC)로 조회
        List<GameRecord> paRecords = boxScoreReader.findPaRecordsByGameId(gameId);
        if (paRecords.isEmpty()) {
            return emptyBoxScore(gameId, game, teamAName, teamBName);
        }

        // 선수 ID → Player(+Team) 맵 (한 번에 JOIN FETCH)
        Set<Long> playerIds = paRecords.stream()
                .flatMap(r -> List.of(r.getBatter().getId(), r.getPitcher().getId()).stream())
                .collect(Collectors.toSet());
        Map<Long, Player> playerMap = playerRepository
                .findAllWithTeamByIdIn(new ArrayList<>(playerIds))
                .stream()
                .collect(Collectors.toMap(Player::getId, p -> p));

        // ── 이닝 수 확정 ─────────────────────────────────────────────
        // halfInningSeq → 실제이닝 = (halfInningSeq + 1) / 2
        // 1부터 ++ 순회: paRecords에 존재하는 최대 실제 이닝 번호
        int totalInnings = paRecords.stream()
                .mapToInt(r -> (r.getInning() + 1) / 2)
                .max()
                .orElse(0);

        // ── 팀별 PA 분리 (player.team.id 기준) ───────────────────────
        List<GameRecord> teamAPAs = paRecords.stream()
                .filter(r -> isOnTeam(r.getBatter().getId(), playerMap, teamAId))
                .toList();
        List<GameRecord> teamBPAs = paRecords.stream()
                .filter(r -> isOnTeam(r.getBatter().getId(), playerMap, teamBId))
                .toList();

        // ── 이닝별 득점 구성 (runsThisPA 합산) ───────────────────────
        // A팀 공격 이닝 수 = totalInnings (9회초도 A팀이 항상 먼저 공격)
        // B팀 공격 이닝 수 = B팀 PA가 있는 마지막 이닝 번호 (홈팀 마감 시 A보다 작을 수 있음)
        int totalInningsB = teamBPAs.stream()
                .mapToInt(r -> (r.getInning() + 1) / 2)
                .max()
                .orElse(0);

        List<Integer> inningScoresA = buildInningScores(teamAPAs, totalInnings);
        List<Integer> inningScoresB = buildInningScores(teamBPAs, totalInningsB);

        // 이전 데이터 호환: runsThisPA가 모두 0이면 DB 저장값(inningScoresA/B 문자열)으로 보완
        if (isAllZero(inningScoresA) && game.getInningScoresA() != null) {
            List<Integer> stored = parseInningScores(game.getInningScoresA());
            if (!stored.isEmpty()) inningScoresA = stored;
        }
        if (isAllZero(inningScoresB) && game.getInningScoresB() != null) {
            List<Integer> stored = parseInningScores(game.getInningScoresB());
            if (!stored.isEmpty()) inningScoresB = stored;
        }

        // ── 타자 박스스코어 ───────────────────────────────────────────
        List<BatterBoxLineDto> teamABatters = buildBatterLines(teamAPAs, playerMap);
        List<BatterBoxLineDto> teamBBatters = buildBatterLines(teamBPAs, playerMap);

        // ── 투수 박스스코어 ───────────────────────────────────────────
        // A팀 투수는 B팀 공격 때 등판 → teamBPAs
        // B팀 투수는 A팀 공격 때 등판 → teamAPAs
        PitcherBoxLineDto pitcherA = buildPitcherLine(teamBPAs, playerMap, teamAName);
        PitcherBoxLineDto pitcherB = buildPitcherLine(teamAPAs, playerMap, teamBName);

        // ── 스코어보드 집계 ───────────────────────────────────────────
        int totalHitsA  = countHits(teamAPAs);
        int totalHitsB  = countHits(teamBPAs);
        int totalWalksA = countResult(teamAPAs, "WALK");
        int totalWalksB = countResult(teamBPAs, "WALK");

        return new BoxScoreDto(
                gameId,
                game.getGameDate() != null ? game.getGameDate().toString() : "-",
                teamAName, teamBName,
                inningScoresA, inningScoresB,
                game.getScoreA(), game.getScoreB(),
                totalHitsA, totalHitsB,
                totalWalksA, totalWalksB,
                teamABatters, teamBBatters,
                pitcherA, pitcherB
        );
    }

    // ─────────────────────────────────────────────────────────────────
    // 내부 빌더
    // ─────────────────────────────────────────────────────────────────

    /**
     * PA 레코드 목록에서 타자 박스스코어 행을 구성합니다.
     * LinkedHashMap으로 최초 등장 순서(=타순)를 보존합니다.
     */
    private List<BatterBoxLineDto> buildBatterLines(
            List<GameRecord> teamPAs, Map<Long, Player> playerMap
    ) {
        Map<Long, List<GameRecord>> byBatter = new LinkedHashMap<>();
        for (GameRecord r : teamPAs) {
            byBatter.computeIfAbsent(r.getBatter().getId(), k -> new ArrayList<>()).add(r);
        }

        List<BatterBoxLineDto> lines = new ArrayList<>();
        int order = 1;
        for (Map.Entry<Long, List<GameRecord>> entry : byBatter.entrySet()) {
            Player player   = playerMap.get(entry.getKey());
            String name     = player != null ? player.getName() : "Unknown";
            String teamName = (player != null && player.getTeam() != null)
                    ? player.getTeam().getName() : "-";

            // 이닝 번호(1~N) → PA 결과 약자
            Map<Integer, String> inningResults = new LinkedHashMap<>();
            for (GameRecord r : entry.getValue()) {
                int actualInning = (r.getInning() + 1) / 2;
                inningResults.put(actualInning, abbreviate(r.getPaResult()));
            }

            lines.add(new BatterBoxLineDto(order++, name, teamName, inningResults));
        }
        return lines;
    }

    /**
     * 상대팀 PA 레코드에서 해당 팀 투수의 경기 박스스코어를 구성합니다.
     * (pitchSequence 합산으로 투구수를 추산합니다.)
     */
    private PitcherBoxLineDto buildPitcherLine(
            List<GameRecord> opponentPAs, Map<Long, Player> playerMap, String pitcherTeamName
    ) {
        if (opponentPAs.isEmpty()) {
            return new PitcherBoxLineDto("-", pitcherTeamName, 0, 0, 0, 0, 0, 0, 0);
        }

        Long pitcherId = opponentPAs.get(0).getPitcher().getId();
        Player pitcher = playerMap.get(pitcherId);
        String name    = pitcher != null ? pitcher.getName() : "Unknown";

        // 아웃 수 → 이닝 계산 (삼진 + 범타)
        int outsRecorded    = countResult(opponentPAs, "STRIKEOUT") + countResult(opponentPAs, "FIELD_OUT");
        int inningsX3       = outsRecorded;
        int hitsAllowed     = countHits(opponentPAs);
        int homeRunsAllowed = countResult(opponentPAs, "HOMERUN");
        int strikeouts      = countResult(opponentPAs, "STRIKEOUT");
        int walks           = countResult(opponentPAs, "WALK");
        int runsAllowed     = opponentPAs.stream()
                .mapToInt(r -> r.getRunsThisPA() != null ? r.getRunsThisPA() : 0)
                .sum();
        // 투구수: 각 타석의 마지막 투구 번호(pitchSequence) 합산으로 추산
        int pitchesThrown   = opponentPAs.stream().mapToInt(GameRecord::getPitchSequence).sum();

        return new PitcherBoxLineDto(name, pitcherTeamName, inningsX3,
                pitchesThrown, hitsAllowed, homeRunsAllowed, strikeouts, walks, runsAllowed);
    }

    // ─────────────────────────────────────────────────────────────────
    // 유틸리티
    // ─────────────────────────────────────────────────────────────────

    /** 팀에 속한 선수 PA인지 player.team.id로 판별합니다. */
    private boolean isOnTeam(Long batterId, Map<Long, Player> playerMap, Long teamId) {
        if (teamId == null) return false;
        Player p = playerMap.get(batterId);
        return p != null && p.getTeam() != null && teamId.equals(p.getTeam().getId());
    }

    /**
     * PA 레코드를 이닝별로 runsThisPA를 합산해 List<Integer>로 반환합니다.
     * runsThisPA가 null인 레코드(중간 투구 또는 이전 데이터)는 0으로 처리합니다.
     */
    private List<Integer> buildInningScores(List<GameRecord> paRecords, int totalInnings) {
        if (totalInnings <= 0) return Collections.emptyList();
        List<Integer> scores = new ArrayList<>(Collections.nCopies(totalInnings, 0));
        for (GameRecord r : paRecords) {
            int idx = (r.getInning() + 1) / 2 - 1;
            if (idx >= 0 && idx < totalInnings && r.getRunsThisPA() != null) {
                scores.set(idx, scores.get(idx) + r.getRunsThisPA());
            }
        }
        return scores;
    }

    private boolean isAllZero(List<Integer> list) {
        return list.stream().allMatch(v -> v == 0);
    }

    private List<Integer> parseInningScores(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .map(Integer::parseInt)
                .collect(Collectors.toList());
    }

    private int countHits(List<GameRecord> records) {
        return (int) records.stream()
                .filter(r -> isHit(r.getPaResult()))
                .count();
    }

    private int countResult(List<GameRecord> records, String resultCode) {
        return (int) records.stream()
                .filter(r -> resultCode.equals(r.getPaResult()))
                .count();
    }

    private boolean isHit(String paResult) {
        return "SINGLE".equals(paResult) || "DOUBLE".equals(paResult)
                || "TRIPLE".equals(paResult) || "HOMERUN".equals(paResult);
    }

    /**
     * PA 결과 코드를 박스스코어 표시용 약자로 변환합니다.
     * STRIKEOUT→SO  FIELD_OUT→FO  WALK→BB  SINGLE→H  DOUBLE→2B  TRIPLE→3B  HOMERUN→HR
     */
    private String abbreviate(String paResult) {
        if (paResult == null) return "?";
        return switch (paResult) {
            case "SINGLE"    -> "H";
            case "DOUBLE"    -> "2B";
            case "TRIPLE"    -> "3B";
            case "HOMERUN"   -> "HR";
            case "STRIKEOUT" -> "SO";
            case "WALK"      -> "BB";
            case "FIELD_OUT" -> "FO";
            default          -> paResult.substring(0, Math.min(2, paResult.length()));
        };
    }

    private BoxScoreDto emptyBoxScore(Long gameId, Game game, String teamAName, String teamBName) {
        return new BoxScoreDto(gameId,
                game.getGameDate() != null ? game.getGameDate().toString() : "-",
                teamAName, teamBName,
                List.of(), List.of(),
                game.getScoreA(), game.getScoreB(),
                0, 0, 0, 0,
                List.of(), List.of(), null, null);
    }
}
