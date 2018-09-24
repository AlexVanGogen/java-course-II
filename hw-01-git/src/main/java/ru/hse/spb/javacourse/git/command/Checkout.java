package ru.hse.spb.javacourse.git.command;

import ru.hse.spb.javacourse.git.entities.Commit;
import ru.hse.spb.javacourse.git.entities.FilesUnstager;
import ru.hse.spb.javacourse.git.entities.Index;
import ru.hse.spb.javacourse.git.entities.RepositoryManager;

import java.io.IOException;
import java.util.List;

public class Checkout extends GitCommand {

    @Override
    public String execute(List<String> args) throws IOException, GitCommandException {
        if (args.isEmpty()) {
            throw new GitCommandException();
        }
        if (args.get(0).equals("--")) {
            List<String> filesToUnstage = args.subList(1, args.size());
            FilesUnstager unstager = new FilesUnstager(filesToUnstage);
            return unstager.unstage();
        } else {
            String revision = args.get(0);
            RepositoryManager.checkout(revision);
            return "Checkout to revision " + revision;
        }
    }
}
