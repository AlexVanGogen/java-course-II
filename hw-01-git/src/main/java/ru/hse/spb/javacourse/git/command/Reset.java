package ru.hse.spb.javacourse.git.command;

import ru.hse.spb.javacourse.git.RepositoryManager;

import java.io.IOException;
import java.util.List;

public class Reset extends GitCommand {

    @Override
    public String execute(List<String> args) throws IOException, GitCommandException {
        if (args.size() != 1) {
            throw new GitCommandException();
        }
        String revision = args.get(0);
        RepositoryManager.reset(revision);
        return "Reset to revision " + revision;
    }
}
