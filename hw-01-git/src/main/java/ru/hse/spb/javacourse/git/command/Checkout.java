package ru.hse.spb.javacourse.git.command;

import ru.hse.spb.javacourse.git.entities.RepositoryManager;

import java.io.IOException;
import java.util.List;

public class Checkout extends GitCommand {

    @Override
    public String execute(List<String> args) throws IOException, GitCommandException {
        if (args.size() != 1) {
            throw new GitCommandException();
        }
        String revision = args.get(0);
        RepositoryManager.checkout(revision);
        return "Checkout to revision " + revision;
    }
}
