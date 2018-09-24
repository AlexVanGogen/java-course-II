package ru.hse.spb.javacourse.git.filestatus;

import org.jetbrains.annotations.NotNull;
import ru.hse.spb.javacourse.git.command.GitCommandException;

public class FileHasNoStatusException extends GitCommandException {

    public FileHasNoStatusException(@NotNull String message) {
        super(message);
    }
}
