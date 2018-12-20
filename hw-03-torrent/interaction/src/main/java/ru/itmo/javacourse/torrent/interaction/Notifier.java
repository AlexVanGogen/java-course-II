package ru.itmo.javacourse.torrent.interaction;

import org.jetbrains.annotations.NotNull;

import java.io.PrintStream;

public class Notifier {

    private static PrintStream output = System.out;

    public static void createMessage(@NotNull String message) {
        output.println(message);
    }
}
