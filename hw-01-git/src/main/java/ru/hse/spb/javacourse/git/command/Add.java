package ru.hse.spb.javacourse.git.command;

import org.jetbrains.annotations.NotNull;
import ru.hse.spb.javacourse.git.Blob;
import ru.hse.spb.javacourse.git.FileAlreadyStagedException;
import ru.hse.spb.javacourse.git.FileNotChangedException;
import ru.hse.spb.javacourse.git.Stage;
import ru.hse.spb.javacourse.git.filestatus.FileStatus;
import ru.hse.spb.javacourse.git.filestatus.StatusChecker;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Add extends GitCommand {

    @NotNull private Stage stage;
    @NotNull private StatusChecker statusChecker;

    public Add() throws IOException {
        stage = Stage.getStage();
        statusChecker = new StatusChecker();
        statusChecker.getActualFileStates();
    }

    @Override
    public String execute(List<String> args) throws IOException, GitCommandException {
        StringBuilder answerBuilder = new StringBuilder();
        int addedFiles = args.size();
        if (args.isEmpty()) {
            throw new GitCommandException();
        }
        for (String nextPath: args) {
            try {
                saveFile(nextPath);
            } catch (FileAlreadyStagedException e) {
                answerBuilder.append("File already staged: ").append(e.getMessage()).append("\n");
                addedFiles--;
            } catch (FileNotChangedException e) {
                answerBuilder.append("File not changed: ").append(e.getMessage()).append("\n");
                addedFiles--;
            }
        }
        stage.writeStage();
        answerBuilder.append("\nAdded ").append(addedFiles).append(" files\n");
        return answerBuilder.toString();
    }

    private void saveFile(@NotNull String pathAsString) throws IOException {
        addToStage(pathAsString);
        Path path = Paths.get(pathAsString);
        Blob blob = new Blob(path);
        blob.save();
    }

    private void addToStage(@NotNull String pathAsString) {
        if (stage.fileInStage(pathAsString)) {
            throw new FileAlreadyStagedException(pathAsString);
        }
        if (statusChecker.getState(pathAsString).equals(FileStatus.UNCHANGED)) {
            throw new FileNotChangedException(pathAsString);
        }
        stage.addNewFile(pathAsString);
    }
}
