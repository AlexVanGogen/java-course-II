package ru.hse.spb.javacourse.git;

import ru.hse.spb.javacourse.git.command.*;
import ru.hse.spb.javacourse.git.command.Commit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Command not specified");
            return;
        }
        String command = args[0];
        List<String> arguments = Arrays.asList(args).subList(1, args.length);

        GitCommand gitCommand;
        try {
            switch (command) {
                case "init":
                    gitCommand = new Init();
                    break;
                case "commit":
                    gitCommand = new Commit();
                    break;
                case "log":
                    gitCommand = new Log();
                    break;
                case "checkout":
                    gitCommand = new Checkout();
                    break;
                case "reset":
                    gitCommand = new Reset();
                    break;
                case "status":
                    gitCommand = new Status();
                    break;
                case "add":
                    gitCommand = new Add();
                    break;
                default:
                    System.out.println("Unknown command: " + command);
                    return;
            }
            System.out.println(gitCommand.execute(arguments));
        } catch (IOException e) {
            System.out.println("Error while processing command: " + e);
        }
    }
}