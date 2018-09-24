package ru.hse.spb.javacourse.git.command;

import ru.hse.spb.javacourse.git.entities.RepositoryAlreadyInitializedException;
import ru.hse.spb.javacourse.git.entities.RepositoryManager;

import java.io.IOException;
import java.util.List;

public class Init extends GitCommand {

    @Override
    public String execute(List<String> args) throws IOException, GitCommandException {
        if (!args.isEmpty()) {
            throw new GitCommandException();
        }
        try {
            RepositoryManager.initialize();
            return "Jgit initialized successfully";
        } catch (RepositoryAlreadyInitializedException e) {
            return "Repository is already initialized";
        }
    }
}
