package ru.hse.spb.javacourse.git.command;

import org.jetbrains.annotations.NotNull;

public class RepositoryNotInMergingStateException extends GitCommandException {
    public RepositoryNotInMergingStateException() {
        super("Repository has not any conflicts");
    }
}
