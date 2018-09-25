package ru.hse.spb.javacourse.git.entities;

import org.jetbrains.annotations.NotNull;
import ru.hse.spb.javacourse.git.filestatus.StatusChecker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class FileRemover {

    @NotNull private List<String> filesToRemove;

    public FileRemover(@NotNull List<String> filesToRemove) {
        this.filesToRemove = filesToRemove;
    }

    @NotNull
    public String removeFiles() throws IOException {
        StringBuilder answerBuilder = new StringBuilder();
        int removedFiles = 0;
        for (String nextFileToRemove: filesToRemove) {
            if (Files.notExists(Paths.get(nextFileToRemove))) {
                answerBuilder.append("File not found: ").append(nextFileToRemove).append("\n");
                continue;
            }
            removeFromStage(nextFileToRemove);
            removeFromIndex(nextFileToRemove);
            removedFiles++;
        }
        StatusChecker statusChecker = new StatusChecker();
        statusChecker.getActualFileStates();
        statusChecker.removeFilesFromState(filesToRemove);
        answerBuilder.append("Removed ").append(removedFiles).append(" files").append("\n");
        return answerBuilder.toString();
    }

    private void removeFromStage(@NotNull String pathToFile) throws IOException {
        Stage stage = Stage.getStage();
        stage.removeFromStageIfExists(pathToFile);
        stage.writeStage();
    }

    private void removeFromIndex(@NotNull String pathToFile) throws IOException {
        Index index = Index.getIndex();
        index.removeBlobsWithPath(pathToFile, true);
        index.writeIndex();
    }
}
