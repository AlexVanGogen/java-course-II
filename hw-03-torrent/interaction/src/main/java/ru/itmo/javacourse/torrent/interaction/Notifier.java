package ru.itmo.javacourse.torrent.interaction;

import org.jetbrains.annotations.NotNull;

import java.io.PrintStream;

public class Notifier {

    private static PrintStream clientOutput = System.out;
    private static PrintStream trackerOutput = System.out;

    public static void createClientMessage(@NotNull String message) {
        clientOutput.println(message);
    }
    public static void createTrackerMessage(@NotNull String message) {
        trackerOutput.println(message);
    }
}
