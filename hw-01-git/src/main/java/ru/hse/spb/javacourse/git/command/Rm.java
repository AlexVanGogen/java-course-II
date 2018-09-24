package ru.hse.spb.javacourse.git.command;

import ru.hse.spb.javacourse.git.entities.FileRemover;

import java.io.IOException;
import java.util.List;

public class Rm extends GitCommand {

    @Override
    public String execute(List<String> args) throws IOException, GitCommandException {
        if (args.isEmpty()) {
            throw new GitCommandException();
        }
        FileRemover remover = new FileRemover(args);
        return remover.removeFiles();
    }
}
