package ru.hse.spb.javacourse.git;

import com.sun.org.apache.xml.internal.security.exceptions.Base64DecodingException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        if (args.length == 0)
            return;
        String command = args[0];

        try {
            if (command.equals("init")) {
                try {
                    RepositoryManager.initialize();
                } catch (RepositoryAlreadyInitializedException e) {
                    System.out.println("Repository is already initialized!");
                }
            }

            if (command.equals("log")) {
                if (args.length > 1)
                    RepositoryManager.showLog(args[1]);
                else
                    RepositoryManager.showLog();
            }

            if (command.equals("commit")) {
                if (args.length == 1) {
                    System.out.println("Please specify commit message");
                    return;
                } else if (args.length == 2) {
                    System.out.println("Nothing to commit");
                }
                String message = args[1];
                List<String> files = new ArrayList<>(Arrays.asList(args).subList(2, args.length));
                RepositoryManager.commit(message, files);
            }

            if (command.equals("checkout")) {
                if (args.length == 1) {
                    System.out.println("Revision isn't specified");
                    return;
                }
                RepositoryManager.checkout(args[1]);
            }

            if (command.equals("reset")) {
                if (args.length == 1) {
                    System.out.println("Revision isn't specified");
                    return;
                }
                RepositoryManager.reset(args[1]);
            }
        } catch (IOException | Base64DecodingException e) {
            System.out.println("Error while processing command: " + e);
        }
    }
}