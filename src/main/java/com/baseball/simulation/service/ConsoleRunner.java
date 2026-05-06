package com.baseball.simulation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConsoleRunner implements CommandLineRunner {

    private final ConsoleManager consoleManager;

    @Override
    public void run(String... args) {
        consoleManager.startInteractiveMenu();
    }
}

