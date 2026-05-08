package com.baseball.simulation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConsoleRunner implements CommandLineRunner {

    private final com.baseball.simulation.controller.ConsoleMenuController consoleMenuController;

    @Override
    public void run(String... args) {
        consoleMenuController.startInteractiveMenu();
    }
}

