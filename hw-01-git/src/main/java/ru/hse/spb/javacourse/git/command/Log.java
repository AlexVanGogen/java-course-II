package ru.hse.spb.javacourse.git.command;

import ru.hse.spb.javacourse.git.RepositoryManager;

import java.io.IOException;
import java.util.List;

public class Log extends GitCommand {

    @Override
    public String execute(List<String> args) throws IOException, GitCommandException {
        if (args.size() > 1) {
            throw new GitCommandException();
        }
        List<String> log = args.isEmpty() ? RepositoryManager.showLog() : RepositoryManager.showLog(args.get(0));
        return String.join("\n", log);
    }
}
