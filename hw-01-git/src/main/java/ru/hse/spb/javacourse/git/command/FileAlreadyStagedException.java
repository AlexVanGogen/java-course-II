package ru.hse.spb.javacourse.git.command;

import org.jetbrains.annotations.NotNull;
import ru.hse.spb.javacourse.git.command.GitCommandException;

public class FileAlreadyStagedException extends GitCommandException {

    public FileAlreadyStagedException(@NotNull String message) {
        super(message);
    }
}
