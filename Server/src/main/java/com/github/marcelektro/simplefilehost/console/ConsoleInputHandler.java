package com.github.marcelektro.simplefilehost.console;

import com.github.marcelektro.simplefilehost.service.auth.AuthService;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Scanner;

@Slf4j
public class ConsoleInputHandler implements Runnable {

    private final AuthService authService;

    public ConsoleInputHandler(AuthService authService) {
        this.authService = authService;
    }


    @Override
    public void run() {
        var s = new Scanner(System.in);

        while (!Thread.currentThread().isInterrupted()) {
            var input = s.nextLine();
            try {
                handleCommand(input);
            } catch (Exception e) {
                log.warn("Failed to handle command: {}", input, e);
            }
        }
    }

    private void handleCommand(String s) {
        var inputSplit = s.split(" ");
        var cmd = inputSplit[0].toLowerCase();
        var args = Arrays.copyOfRange(inputSplit, 1, inputSplit.length);

        switch (cmd) {
            case "help" -> log.info("""
                    Available commands:
                    - exit
                    - createUser <id> <username> <password>"""
            );

            case "exit", "quit" -> {
                log.info("Exiting console input handler and application.");
                Thread.currentThread().interrupt();
                System.exit(0);
            }

            case "createuser" -> {

                if (args.length < 3) {
                    log.warn("Usage: createUser <id> <username> <password>");
                    return;
                }

                var id = args[0];
                var username = args[1];
                var password = args[2];

                try {
                    var result = authService.registerUser(id, username, password);

                    if (result.isSuccess()) {
                        log.info("User created successfully with ID: {}", result.getData());
                    } else {
                        log.warn("Failed to create user {}: {}", result.getErrorCode(), result.getMessage());
                    }

                } catch (Exception e) {
                    log.error("Error creating user", e);
                }

            }

            default -> log.warn("Unknown command: {}. Use `help` for help.", cmd);
        }
    }

}
