package ru.hse.spb.javacourse.git.entities;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import ru.hse.spb.javacourse.git.NothingToCommitException;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static ru.hse.spb.javacourse.git.entities.RepositoryManager.GIT_STAGE_PATH;

public class Stage {

    private List<String> stagedFilePaths;

    private Stage() {
        stagedFilePaths = new ArrayList<>();
    }

    public static Stage getStage() throws IOException {
        Stage stage = new Stage();
        JSONArray stagedFiles = new JSONArray(String.join("\n", Files.readAllLines(GIT_STAGE_PATH)));
        for (Object nextFile: stagedFiles) {
            stage.stagedFilePaths.add((String)nextFile);
        }
        return stage;
    }

    public void writeStage() throws IOException {
        Files.write(GIT_STAGE_PATH, Collections.singletonList(new JSONArray(stagedFilePaths).toString()));
    }

    public void resetStage() throws IOException {
        Files.write(GIT_STAGE_PATH, Collections.singletonList(new JSONArray().toString()));
    }

    public void addNewFile(@NotNull String pathAsString) {
        stagedFilePaths.add(pathAsString);
    }

    public List<String> getStagedFilePaths() {
        return stagedFilePaths;
    }

    public void commitStage(@NotNull String message) throws IOException, NothingToCommitException {
        Commit.makeAndSubmitStaged(message, this);
    }

    public void removeFromStageIfExists(@NotNull String pathAsString) {
        stagedFilePaths.remove(pathAsString);
    }

    public boolean fileInStage(@NotNull String pathAsString) {
        return stagedFilePaths.contains(pathAsString);
    }
}
