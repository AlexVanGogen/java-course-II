package ru.hse.spb.javacourse.git.command;

import ru.hse.spb.javacourse.git.entities.MergingState;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class GitCommand {

    public String execute(List<String> args) throws IOException, GitCommandException {
        if (args.equals(Collections.singletonList("--continue")) && MergingState.isRepositoryInMergingState()) {
            throw new RepositoryInMergingStateException();
        }
        return null;
    }
}
