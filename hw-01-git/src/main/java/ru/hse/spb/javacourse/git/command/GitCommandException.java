package ru.hse.spb.javacourse.git.command;

import org.jetbrains.annotations.NotNull;

public class GitCommandException extends IllegalArgumentException {

    public GitCommandException() { }

    public GitCommandException(@NotNull String message) {
        super(message);
    }
}
