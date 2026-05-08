package com.baseball.simulation.controller;

import com.baseball.simulation.domain.GameScheduleItem;
import com.baseball.simulation.domain.dto.BatterRankingDto;
import com.baseball.simulation.domain.dto.PitcherRankingDto;
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
                case Integer menu when menu == 2 -> printGameRecordsAndSchedule();
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
        System.out.println("1) 랜덤 모드");
        System.out.println("2) 승/패 모드");
        System.out.println("3) 스코어 모드");
        System.out.print("선택: ");

        Object parsedMode = parseInput(scanner.nextLine());
        switch (parsedMode) {
            case Integer mode when mode == 1 -> gameFacade.startRandomGame();
            case Integer mode when mode == 2 -> System.out.println("승/패 모드는 준비 중입니다.");
            case Integer mode when mode == 3 -> System.out.println("스코어 모드는 준비 중입니다.");
            default -> System.out.println("올바른 모드 번호를 입력해주세요.");
        }
    }

    // -----------------------------------------------------------------------
    // 기록/일정 메뉴
    // -----------------------------------------------------------------------

    private void printGameRecordsAndSchedule() {
        System.out.println();
        System.out.println("=== 기록/일정 ===");

        List<GameScheduleItem> items = gameFacade.getRecentGames();
        if (items.isEmpty()) {
            System.out.println("등록된 경기가 없습니다.");
            return;
        }

        for (GameScheduleItem item : items) {
            System.out.printf("%s %d:%d %s (Game_%d 경기)%n",
                    item.awayTeamName(), item.awayScore(),
                    item.homeScore(), item.homeTeamName(),
                    item.gameId());
        }
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

            // 정렬 방향 선택
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
        System.out.println("10. OPS     0. 뒤로");
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
            default -> Comparator.comparingDouble(BatterRankingDto::battingAvg);
        };
        return descending ? base.reversed() : base;
    }

    private void printBatterRankings(
            int seasonYear,
            String statLabel,
            boolean descending,
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
            Object dirChoice  = parseInput(scanner.nextLine());
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
            int seasonYear,
            String statLabel,
            boolean descending,
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
