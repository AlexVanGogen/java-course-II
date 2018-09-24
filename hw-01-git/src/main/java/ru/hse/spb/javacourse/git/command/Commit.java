package ru.hse.spb.javacourse.git.command;

import ru.hse.spb.javacourse.git.NothingToCommitException;
import ru.hse.spb.javacourse.git.RepositoryManager;
import ru.hse.spb.javacourse.git.Stage;

import java.io.IOException;
import java.util.List;

public class Commit extends GitCommand {

    @Override
    public String execute(List<String> args) throws IOException, GitCommandException {
        if (args.size() < 1) {
            throw new GitCommandException();
        }
        try {
            if (args.size() == 1) {
                Stage stage = Stage.getStage();
                stage.commitStage(args.get(0));
            } else {
                String message = args.get(0);
                List<String> files = args.subList(1, args.size());
                RepositoryManager.commit(message, files);
            }
        } catch (NothingToCommitException e) {
            return "Nothing to commit";
        }
        return "Committed successfully";
    }
}
