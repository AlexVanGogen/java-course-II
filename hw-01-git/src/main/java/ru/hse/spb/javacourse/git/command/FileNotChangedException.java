package ru.hse.spb.javacourse.git.command;

import org.jetbrains.annotations.NotNull;
import ru.hse.spb.javacourse.git.command.GitCommandException;

public class FileNotChangedException extends GitCommandException {

    public FileNotChangedException(@NotNull String message) {
        super(message);
    }
}
