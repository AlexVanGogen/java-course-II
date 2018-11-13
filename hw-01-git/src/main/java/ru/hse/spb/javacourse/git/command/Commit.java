package ru.hse.spb.javacourse.git.command;

import ru.hse.spb.javacourse.git.entities.NothingToCommitException;
import ru.hse.spb.javacourse.git.entities.RepositoryManager;
import ru.hse.spb.javacourse.git.entities.Stage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Commit extends GitCommand {

    @Override
    public String execute(List<String> args) throws IOException, GitCommandException {
        if (args.size() < 1) {
            throw new GitCommandException();
        }
        super.execute(args);
        try {
            if (args.size() == 1) {
                Stage stage = Stage.getStage();
                stage.commitStage(args.get(0));
            } else {
                String message = args.get(0);
                List<String> files = args.subList(1, args.size());
                for (String nextPath: files) {
                    if (Files.notExists(Paths.get(nextPath))) {
                        return "File not found: " + nextPath;
                    }
                }
                RepositoryManager.commit(message, files);
            }
        } catch (NothingToCommitException e) {
            return "Nothing to commit";
        }
        return "Committed successfully";
    }
}
