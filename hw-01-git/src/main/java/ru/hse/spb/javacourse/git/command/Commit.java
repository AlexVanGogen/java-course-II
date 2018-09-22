package ru.hse.spb.javacourse.git.command;

import ru.hse.spb.javacourse.git.RepositoryManager;

import java.io.IOException;
import java.util.List;

public class Commit extends GitCommand {

    @Override
    public String execute(List<String> args) throws IOException, GitCommandException {
        if (args.size() < 2) {
            throw new GitCommandException();
        }
        String message = args.get(0);
        List<String> files = args.subList(1, args.size());
        RepositoryManager.commit(message, files);
        return "Committed successfully";
    }
}
