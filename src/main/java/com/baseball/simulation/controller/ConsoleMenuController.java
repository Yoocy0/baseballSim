package com.baseball.simulation.controller;

import com.baseball.simulation.domain.GameScheduleItem;
import com.baseball.simulation.facade.GameFacade;
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
                case Integer menu when menu == 3 -> System.out.println("순위 기능은 준비 중입니다.");
                case Integer menu when menu == 0 -> {
                    System.out.println("프로그램을 종료합니다.");
                    return;
                }
                default -> System.out.println("올바른 메뉴 번호를 입력해주세요.");
            }
        }
    }

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
                    item.awayTeamName(),
                    item.awayScore(),
                    item.homeScore(),
                    item.homeTeamName(),
                    item.gameId());
        }
    }

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
