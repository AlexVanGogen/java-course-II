package ru.hse.spb.javacourse.git.entities;

import org.jetbrains.annotations.NotNull;
import ru.hse.spb.javacourse.git.filestatus.FileStatus;
import ru.hse.spb.javacourse.git.filestatus.StatusChecker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

public class FileUnstager {

    @NotNull private List<String> filesToUnstage;
    @NotNull private StatusChecker statusChecker;
    @NotNull private Stage stage;
    @NotNull private Index index;

    public FileUnstager(@NotNull List<String> filesToUnstage) throws IOException {
        this.filesToUnstage = filesToUnstage;
        statusChecker = new StatusChecker();
        statusChecker.getActualFileStates();
        stage = Stage.getStage();
        index = Index.getIndex();
    }

    public String unstage() throws IOException {
        StringBuilder answerBuilder = new StringBuilder();
        int unstagedFiles = 0;
        for (String nextFileToUnstage: filesToUnstage) {
            if (Files.notExists(Paths.get(nextFileToUnstage))) {
                answerBuilder.append("File not found: ").append(nextFileToUnstage).append("\n");
                continue;
            }
            if (statusChecker.getState(nextFileToUnstage).equals(FileStatus.STAGED)) {
                Commit latestCommit = Commit.ofHead();
                if (latestCommit == null) {
                    answerBuilder.append("File has not previous versions: ").append(nextFileToUnstage).append("\n");
                    continue;
                }
                Blob latestSavedVersion = index.findLatestVersionOfFile(Paths.get(nextFileToUnstage));
                Blob previousSavedVersion = index.findPreviousVersionOfFile(latestSavedVersion);
                if (previousSavedVersion == null) {
                    answerBuilder.append("File has not previous versions: ").append(nextFileToUnstage).append("\n");
                    continue;
                }
                Files.write(Paths.get(nextFileToUnstage), Collections.singletonList(previousSavedVersion.decodeContents()));
                index.removeBlobIfExists(latestSavedVersion, false);
                unstagedFiles++;
            } else if (statusChecker.getState(nextFileToUnstage).equals(FileStatus.MODIFIED)) {
                Blob latestSavedVersion = index.findLatestVersionOfFile(Paths.get(nextFileToUnstage));
                if (latestSavedVersion == null) {
                    answerBuilder.append("File has not any saved versions: ").append(nextFileToUnstage).append("\n");
                    continue;
                }
                Files.write(Paths.get(nextFileToUnstage), Collections.singletonList(latestSavedVersion.decodeContents()));
                unstagedFiles++;
            } else {
                answerBuilder.append("File is not staged or modified: ").append(nextFileToUnstage).append("\n");
            }
        }
        answerBuilder.append("Unstaged ").append(unstagedFiles).append(" files").append("\n");
        return answerBuilder.toString();
    }

    @NotNull
    public Index getIndex() {
        return index;
    }
}
