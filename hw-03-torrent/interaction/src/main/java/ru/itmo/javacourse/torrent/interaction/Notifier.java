package ru.itmo.javacourse.torrent.interaction;

import org.jetbrains.annotations.NotNull;

import java.io.PrintStream;

public class Notifier {

    public static void createMessage(@NotNull String message, @NotNull PrintStream output) {
        output.println(message);
    }
}
