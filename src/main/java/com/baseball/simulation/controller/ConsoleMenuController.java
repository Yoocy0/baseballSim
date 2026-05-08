package com.baseball.simulation.controller;

import com.baseball.simulation.domain.dto.BatterBoxLineDto;
import com.baseball.simulation.domain.dto.BatterRankingDto;
import com.baseball.simulation.domain.dto.BoxScoreDto;
import com.baseball.simulation.domain.dto.GameListItemDto;
import com.baseball.simulation.domain.dto.PitcherBoxLineDto;
import com.baseball.simulation.domain.dto.PitcherRankingDto;
import com.baseball.simulation.domain.dto.TeamDto;
import com.baseball.simulation.domain.dto.TeamRankingDto;
import com.baseball.simulation.facade.GameFacade;
import java.time.Year;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 콘솔 메뉴 컨트롤러입니다.
 * GameFacade만 주입받으며, Service / Repository에 직접 의존하지 않습니다.
 */
@Component
@RequiredArgsConstructor
public class ConsoleMenuController {

    private final GameFacade gameFacade;

    public void startInteractiveMenu() {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            printMainMenu();
            Object parsedMain = parseInput(scanner.nextLine());

            switch (parsedMain) {
                case Integer menu when menu == 1 -> handleGameMenu(scanner);
                case Integer menu when menu == 2 -> handleGameScheduleMenu(scanner);
                case Integer menu when menu == 3 -> handleRankingsMenu(scanner);
                case Integer menu when menu == 0 -> {
                    System.out.println("프로그램을 종료합니다.");
                    return;
                }
                default -> System.out.println("올바른 메뉴 번호를 입력해주세요.");
            }
        }
    }

    // -----------------------------------------------------------------------
    // 경기 메뉴
    // -----------------------------------------------------------------------

    private void handleGameMenu(Scanner scanner) {
        System.out.println();
        System.out.println("=== 경기 모드 선택 ===");
        System.out.println("1. 일반 모드");
        System.out.println("2. 승/패 지정 모드");
        System.out.println("3. 스코어 모드");
        System.out.print("선택: ");

        Object parsedMode = parseInput(scanner.nextLine());
        switch (parsedMode) {
            case Integer mode when mode == 1 -> gameFacade.startRandomGame();
            case Integer mode when mode == 2 -> handleWinControlMode(scanner);
            case Integer mode when mode == 3 -> handleScoreMode(scanner);
            default -> System.out.println("올바른 모드 번호를 입력해주세요.");
        }
    }

    private void handleWinControlMode(Scanner scanner) {
        List<TeamDto> teams = gameFacade.getTeamList();
        if (teams.isEmpty()) {
            System.out.println("등록된 팀이 없습니다.");
            return;
        }

        System.out.println();
        System.out.println("=== 승리 팀 선택 ===");
        for (int i = 0; i < teams.size(); i++) {
            System.out.printf("%d. %s%n", i + 1, teams.get(i).name());
        }
        System.out.print("승리할 팀 번호 선택: ");

        Object input = parseInput(scanner.nextLine());
        if (input instanceof Integer n && n >= 1 && n <= teams.size()) {
            TeamDto selectedTeam = teams.get(n - 1);
            System.out.printf("[시스템] '%s' 팀의 승리가 보장된 경기를 시작합니다.%n", selectedTeam.name());
            gameFacade.startWinControlGame(selectedTeam.id());
        } else {
            System.out.println("올바른 번호를 입력해주세요.");
        }
    }

    private void handleScoreMode(Scanner scanner) {
        List<TeamDto> teams = gameFacade.getTeamList();
        if (teams.size() < 2) {
            System.out.println("팀이 2개 이상 등록되어야 합니다.");
            return;
        }
        TeamDto teamA = teams.get(0);
        TeamDto teamB = teams.get(1);

        System.out.println();
        System.out.println("=== 스코어 모드 — 타겟 점수 입력 ===");
        System.out.println("※ 0 이상의 정수만 입력 가능합니다. 음수·소수는 재입력을 요청합니다.");
        System.out.println();

        System.out.printf("A팀 (%s) 타겟 점수: ", teamA.name());
        int targetA = readNonNegativeInt(scanner);

        System.out.printf("B팀 (%s) 타겟 점수: ", teamB.name());
        int targetB = readNonNegativeInt(scanner);

        System.out.printf("%n[스코어 모드] %s %d : %d %s 타겟으로 시뮬레이션을 시작합니다.%n",
                teamA.name(), targetA, targetB, teamB.name());
        gameFacade.startScoreGame(targetA, targetB);
    }

    /**
     * 0 이상의 정수를 입력받을 때까지 반복합니다.
     * 음수·소수·문자 입력 시 오류 메시지를 출력하고 재입력을 요청합니다.
     */
    private int readNonNegativeInt(Scanner scanner) {
        while (true) {
            String raw = scanner.nextLine().trim();
            try {
                // 소수점 포함 여부 먼저 확인
                if (raw.contains(".")) {
                    System.out.print("소수는 입력할 수 없습니다. 0 이상의 정수를 입력해주세요: ");
                    continue;
                }
                int value = Integer.parseInt(raw);
                if (value >= 0) return value;
                System.out.print("음수는 입력할 수 없습니다. 0 이상의 정수를 입력해주세요: ");
            } catch (NumberFormatException e) {
                System.out.print("올바른 숫자가 아닙니다. 0 이상의 정수를 입력해주세요: ");
            }
        }
    }

    // -----------------------------------------------------------------------
    // 기록/일정 메뉴 (경기 목록 → 경기 선택 → 박스스코어)
    // -----------------------------------------------------------------------

    private void handleGameScheduleMenu(Scanner scanner) {
        while (true) {
            List<GameListItemDto> games = gameFacade.getGameList();

            System.out.println();
            System.out.println("=== 기록/일정 ===");

            if (games.isEmpty()) {
                System.out.println("아직 진행된 경기가 없습니다.");
                return;
            }

            for (int i = 0; i < games.size(); i++) {
                System.out.println(games.get(i).toDisplayLine(i + 1));
            }
            System.out.println();
            System.out.print("경기 번호 입력 (0: 이전): ");

            Object input = parseInput(scanner.nextLine());
            if (input instanceof Integer n) {
                if (n == 0) return;
                if (n >= 1 && n <= games.size()) {
                    Long selectedGameId = games.get(n - 1).gameId();
                    showBoxScore(selectedGameId);
                } else {
                    System.out.println("올바른 번호를 입력해주세요.");
                }
            } else {
                System.out.println("숫자를 입력해주세요.");
            }
        }
    }

    private void showBoxScore(Long gameId) {
        try {
            BoxScoreDto bs = gameFacade.getBoxScore(gameId);
            printBoxScore(bs);
        } catch (Exception e) {
            System.out.println("박스 스코어를 불러오는 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    private void printBoxScore(BoxScoreDto bs) {
        int totalInnings = Math.max(bs.inningScoresA().size(), bs.inningScoresB().size());

        // ── 헤더 ──────────────────────────────────────────────────────────────
        System.out.println();
        String divider = "═".repeat(62);
        System.out.println(divider);
        System.out.printf("  %s | %s vs %s%n", bs.gameDate(), bs.teamAName(), bs.teamBName());
        System.out.println(divider);

        // ── 전광판 스코어보드 ──────────────────────────────────────────────────
        System.out.println();
        System.out.println("[전광판]");

        StringBuilder sbHeader = new StringBuilder(String.format("%-10s|", ""));
        for (int i = 1; i <= totalInnings; i++) {
            sbHeader.append(String.format(" %2d", i));
        }
        sbHeader.append("  |  R  H BB");
        String headerLine = sbHeader.toString();
        System.out.println(headerLine);
        System.out.println("─".repeat(headerLine.length()));

        System.out.print(buildScoreboardRow(bs.teamAName(), bs.inningScoresA(), totalInnings, false,
                bs.totalRunsA(), bs.totalHitsA(), bs.totalWalksA()));
        System.out.print(buildScoreboardRow(bs.teamBName(), bs.inningScoresB(), totalInnings, true,
                bs.totalRunsB(), bs.totalHitsB(), bs.totalWalksB()));
        System.out.println("─".repeat(headerLine.length()));

        // ── 타자 박스스코어 ────────────────────────────────────────────────────
        if (!bs.teamABatters().isEmpty()) {
            printBatterSection(bs.teamAName(), bs.teamABatters(), totalInnings);
        }
        if (!bs.teamBBatters().isEmpty()) {
            printBatterSection(bs.teamBName(), bs.teamBBatters(), totalInnings);
        }

        // ── 투수 박스스코어 ────────────────────────────────────────────────────
        System.out.println();
        System.out.println("[투수 박스스코어]");
        String pitcherHeader = String.format("%-4s %-18s | %-5s | %5s | %5s | %5s | %5s | %5s | %5s",
                "", "선수명(팀)", "이닝", "투구수", "피안타", "피홈런", "탈삼진", "볼넷", "실점");
        System.out.println("─".repeat(pitcherHeader.length()));
        System.out.println(pitcherHeader);
        System.out.println("─".repeat(pitcherHeader.length()));
        printPitcherBoxLine("[A]", bs.teamAPitcher());
        printPitcherBoxLine("[B]", bs.teamBPitcher());
        System.out.println("─".repeat(pitcherHeader.length()));
    }

    private String buildScoreboardRow(
            String teamName,
            List<Integer> inningScores,
            int totalInnings,
            boolean isHomeTeam,
            int totalRuns, int totalHits, int totalWalks
    ) {
        StringBuilder row = new StringBuilder(String.format("%-10s|", teamName));
        for (int i = 0; i < totalInnings; i++) {
            if (i < inningScores.size()) {
                row.append(String.format(" %2d", inningScores.get(i)));
            } else if (isHomeTeam) {
                row.append("  X");   // 홈팀이 마지막 이닝 말을 치지 않음
            } else {
                row.append("  -");
            }
        }
        row.append(String.format("  | %2d %2d %2d%n", totalRuns, totalHits, totalWalks));
        return row.toString();
    }

    /**
     * 타자 박스스코어 출력 (이닝별 PA 결과만 표시, 안타/타점 없음)
     * 결과 약자: SO=삼진  FO=범타  BB=볼넷  H=단타  2B=2루타  3B=3루타  HR=홈런
     */
    private void printBatterSection(
            String teamName, List<BatterBoxLineDto> batters, int totalInnings
    ) {
        System.out.println();
        System.out.printf("[타자 박스스코어 - %s]%n", teamName);

        // 헤더
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%2s %-16s|", "#", "선수명"));
        for (int i = 1; i <= totalInnings; i++) {
            sb.append(String.format(" %3d", i));
        }
        System.out.println(sb);
        System.out.println("─".repeat(sb.length()));

        for (BatterBoxLineDto batter : batters) {
            StringBuilder row = new StringBuilder();
            row.append(String.format("%2d %-16s|", batter.battingOrder(), batter.playerName()));
            for (int i = 1; i <= totalInnings; i++) {
                String result = batter.inningResults().getOrDefault(i, "-");
                row.append(String.format(" %3s", result));
            }
            System.out.println(row);
        }
        System.out.println("─".repeat(sb.length()));
    }

    private void printPitcherBoxLine(String tag, PitcherBoxLineDto p) {
        if (p == null) return;
        System.out.printf("%-4s %-18s | %-5s | %5d | %5d | %5d | %5d | %5d | %5d%n",
                tag,
                p.playerName() + "(" + p.teamName() + ")",
                p.inningsDisplay(),
                p.pitchesThrown(),
                p.hitsAllowed(),
                p.homeRunsAllowed(),
                p.strikeouts(),
                p.walks(),
                p.runsAllowed());
    }

    // -----------------------------------------------------------------------
    // 순위 메뉴
    // -----------------------------------------------------------------------

    private void handleRankingsMenu(Scanner scanner) {
        while (true) {
            System.out.println();
            System.out.println("=== 순위 ===");
            System.out.println("1. 팀 순위");
            System.out.println("2. 타자 순위");
            System.out.println("3. 투수 순위");
            System.out.println("0. 메인 메뉴로");
            System.out.print("선택: ");

            Object parsed = parseInput(scanner.nextLine());
            switch (parsed) {
                case Integer menu when menu == 1 -> printTeamRankings();
                case Integer menu when menu == 2 -> handleBatterRankingsMenu(scanner);
                case Integer menu when menu == 3 -> handlePitcherRankingsMenu(scanner);
                case Integer menu when menu == 0 -> { return; }
                default -> System.out.println("올바른 메뉴 번호를 입력해주세요.");
            }
        }
    }

    private void printTeamRankings() {
        int seasonYear = Year.now().getValue();
        System.out.println();
        System.out.printf("=== %d 시즌 팀 순위 ===%n", seasonYear);

        List<TeamRankingDto> rankings = gameFacade.getSeasonRankings(seasonYear);
        if (rankings.isEmpty()) {
            System.out.println("아직 기록된 시즌 성적이 없습니다.");
            return;
        }

        System.out.println("────────────────────────────────────────");
        for (int i = 0; i < rankings.size(); i++) {
            System.out.println(rankings.get(i).toDisplayLine(i + 1));
        }
        System.out.println("────────────────────────────────────────");
    }

    // -----------------------------------------------------------------------
    // 타자 순위 메뉴
    // -----------------------------------------------------------------------

    private void handleBatterRankingsMenu(Scanner scanner) {
        int seasonYear = Year.now().getValue();

        while (true) {
            printBatterStatMenu();
            Object statChoice = parseInput(scanner.nextLine());

            if (statChoice instanceof Integer s && s == 0) return;

            String statLabel = getBatterStatLabel(statChoice);
            if (statLabel == null) {
                System.out.println("올바른 번호를 입력해주세요.");
                continue;
            }

            System.out.println();
            System.out.println("정렬 방향을 선택하세요:");
            System.out.println("1. 내림차순 (높은 값 우선)");
            System.out.println("2. 오름차순 (낮은 값 우선)");
            System.out.print("선택: ");
            Object dirChoice = parseInput(scanner.nextLine());
            boolean descending = !(dirChoice instanceof Integer d && d == 2);

            Comparator<BatterRankingDto> comparator = buildComparator(statChoice, descending);
            List<BatterRankingDto> rankings = gameFacade.getBatterRankings(seasonYear, comparator);

            printBatterRankings(seasonYear, statLabel, descending, rankings);
        }
    }

    private void printBatterStatMenu() {
        System.out.println();
        System.out.printf("=== 타자 순위 기준 선택 (0: 뒤로) ===%n");
        System.out.println(" 1. 타율    2. 안타    3. 볼넷");
        System.out.println(" 4. 2루타   5. 3루타   6. 홈런");
        System.out.println(" 7. 삼진    8. 출루율  9. 장타율");
        System.out.println("10. OPS    11. 득점   12. 타점");
        System.out.println(" 0. 뒤로");
        System.out.print("선택: ");
    }

    private String getBatterStatLabel(Object choice) {
        if (!(choice instanceof Integer n)) return null;
        return switch (n) {
            case  1 -> "타율";
            case  2 -> "안타";
            case  3 -> "볼넷";
            case  4 -> "2루타";
            case  5 -> "3루타";
            case  6 -> "홈런";
            case  7 -> "삼진";
            case  8 -> "출루율";
            case  9 -> "장타율";
            case 10 -> "OPS";
            case 11 -> "득점";
            case 12 -> "타점";
            default -> null;
        };
    }

    private Comparator<BatterRankingDto> buildComparator(Object choice, boolean descending) {
        Comparator<BatterRankingDto> base = switch ((Integer) choice) {
            case  1 -> Comparator.comparingDouble(BatterRankingDto::battingAvg);
            case  2 -> Comparator.comparingInt(BatterRankingDto::hits);
            case  3 -> Comparator.comparingInt(BatterRankingDto::walks);
            case  4 -> Comparator.comparingInt(BatterRankingDto::doubles);
            case  5 -> Comparator.comparingInt(BatterRankingDto::triples);
            case  6 -> Comparator.comparingInt(BatterRankingDto::homeRuns);
            case  7 -> Comparator.comparingInt(BatterRankingDto::strikeouts);
            case  8 -> Comparator.comparingDouble(BatterRankingDto::onBasePct);
            case  9 -> Comparator.comparingDouble(BatterRankingDto::sluggingPct);
            case 10 -> Comparator.comparingDouble(BatterRankingDto::ops);
            case 11 -> Comparator.comparingInt(BatterRankingDto::runs);
            case 12 -> Comparator.comparingInt(BatterRankingDto::rbi);
            default -> Comparator.comparingDouble(BatterRankingDto::battingAvg);
        };
        return descending ? base.reversed() : base;
    }

    private void printBatterRankings(
            int seasonYear, String statLabel, boolean descending,
            List<BatterRankingDto> rankings
    ) {
        System.out.println();
        System.out.printf("=== %d 시즌 타자 순위 [기준: %s / %s] ===%n",
                seasonYear, statLabel, descending ? "내림차순" : "오름차순");

        if (rankings.isEmpty()) {
            System.out.println("아직 기록된 타자 성적이 없습니다.");
            return;
        }

        System.out.println("────────────────────────────────────────────────────────────────────────────────────");
        for (int i = 0; i < rankings.size(); i++) {
            System.out.println(rankings.get(i).toDisplayLine(i + 1));
        }
        System.out.println("────────────────────────────────────────────────────────────────────────────────────");
    }

    // -----------------------------------------------------------------------
    // 투수 순위 메뉴
    // -----------------------------------------------------------------------

    private void handlePitcherRankingsMenu(Scanner scanner) {
        int seasonYear = Year.now().getValue();

        while (true) {
            printPitcherStatMenu();
            Object statChoice = parseInput(scanner.nextLine());

            if (statChoice instanceof Integer s && s == 0) return;

            String statLabel = getPitcherStatLabel(statChoice);
            if (statLabel == null) {
                System.out.println("올바른 번호를 입력해주세요.");
                continue;
            }

            System.out.println();
            System.out.println("정렬 방향을 선택하세요:");
            System.out.println("1. 내림차순 (높은 값 우선)");
            System.out.println("2. 오름차순 (낮은 값 우선)");
            System.out.print("선택: ");
            Object dirChoice   = parseInput(scanner.nextLine());
            boolean descending = !(dirChoice instanceof Integer d && d == 2);

            Comparator<PitcherRankingDto> comparator = buildPitcherComparator(statChoice, descending);
            List<PitcherRankingDto> rankings = gameFacade.getPitcherRankings(seasonYear, comparator);

            printPitcherRankings(seasonYear, statLabel, descending, rankings);
        }
    }

    private void printPitcherStatMenu() {
        System.out.println();
        System.out.println("=== 투수 순위 기준 선택 (0: 뒤로) ===");
        System.out.println("1. 평균자책점(ERA)   2. 승리");
        System.out.println("3. 탈삼진            4. 이닝");
        System.out.println("5. 투구수            6. 볼넷");
        System.out.println("0. 이전으로");
        System.out.print("선택: ");
    }

    private String getPitcherStatLabel(Object choice) {
        if (!(choice instanceof Integer n)) return null;
        return switch (n) {
            case 1 -> "평균자책점(ERA)";
            case 2 -> "승리";
            case 3 -> "탈삼진";
            case 4 -> "이닝";
            case 5 -> "투구수";
            case 6 -> "볼넷";
            default -> null;
        };
    }

    private Comparator<PitcherRankingDto> buildPitcherComparator(Object choice, boolean descending) {
        Comparator<PitcherRankingDto> base = switch ((Integer) choice) {
            case 1 -> Comparator.comparingDouble(PitcherRankingDto::era);
            case 2 -> Comparator.comparingInt(PitcherRankingDto::wins);
            case 3 -> Comparator.comparingInt(PitcherRankingDto::strikeouts);
            case 4 -> Comparator.comparingInt(PitcherRankingDto::inningsX3);
            case 5 -> Comparator.comparingInt(PitcherRankingDto::pitchesThrown);
            case 6 -> Comparator.comparingInt(PitcherRankingDto::walks);
            default -> Comparator.comparingDouble(PitcherRankingDto::era);
        };
        return descending ? base.reversed() : base;
    }

    private void printPitcherRankings(
            int seasonYear, String statLabel, boolean descending,
            List<PitcherRankingDto> rankings
    ) {
        System.out.println();
        System.out.printf("=== %d 시즌 투수 순위 [기준: %s / %s] ===%n",
                seasonYear, statLabel, descending ? "내림차순" : "오름차순");

        if (rankings.isEmpty()) {
            System.out.println("아직 기록된 투수 성적이 없습니다.");
            return;
        }

        System.out.println("──────────────────────────────────────────────────────────────────────────────");
        for (int i = 0; i < rankings.size(); i++) {
            System.out.println(rankings.get(i).toDisplayLine(i + 1));
        }
        System.out.println("──────────────────────────────────────────────────────────────────────────────");
    }

    // -----------------------------------------------------------------------
    // 공통 유틸
    // -----------------------------------------------------------------------

    private void printMainMenu() {
        System.out.println();
        System.out.println("=== 메인 메뉴 ===");
        System.out.println("1. 경기 (Game Start)");
        System.out.println("2. 기록/일정 (Records & Schedule)");
        System.out.println("3. 순위 (Rankings)");
        System.out.println("0. 종료 (Exit)");
        System.out.print("선택: ");
    }

    private Object parseInput(String rawInput) {
        try {
            return Integer.parseInt(rawInput.trim());
        } catch (NumberFormatException e) {
            return rawInput;
        }
    }
}
