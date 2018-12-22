package ru.itmo.javacourse.torrent.command;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

public interface AbstractCommand {

    void execute(@NotNull List<String> args, @NotNull PrintStream output) throws IOException;
}
