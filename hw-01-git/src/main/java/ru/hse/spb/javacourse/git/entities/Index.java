package ru.hse.spb.javacourse.git.entities;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static ru.hse.spb.javacourse.git.entities.RepositoryManager.GIT_INDEX_PATH;

public class Index {

    @NotNull List<Blob> indexedFiles;

    private Index() {
        indexedFiles = new ArrayList<>();
    }

    @NotNull
    public static Index getIndex() throws IOException {
        Index index = new Index();
        for (JSONObject nextBlobIndex : Files.lines(GIT_INDEX_PATH).map(JSONObject::new).collect(Collectors.toList())) {
            index.indexedFiles.add(new Blob(nextBlobIndex));
        }
        return index;
    }

    public void writeIndex() throws IOException {
        Files.write(
                GIT_INDEX_PATH,
                indexedFiles.stream().map(Blob::toJson).map(JSONObject::toString).collect(Collectors.toList())
        );
    }

    public void updateIndex(int numberOfFilesChangedByRevertedCommit) {
        indexedFiles = indexedFiles.subList(0, indexedFiles.size() - numberOfFilesChangedByRevertedCommit);
    }

    @Nullable
    public Blob findLatestVersionOfFile(@NotNull Path pathToFile) {
        for (int i = indexedFiles.size() - 1; i >= 0; i--) {
            if (indexedFiles.get(i).getObjectQualifiedPath().equals(pathToFile)) {
                return indexedFiles.get(i);
            }
        }
        return null;
    }

    @Nullable
    public Blob findPreviousVersionOfFile(@NotNull Commit latestCommit, @NotNull Blob currentFileVersion) {
        for (int i = indexedFiles.size() - latestCommit.getNumberOfCommittedFiles() - 1; i >= 0; i--) {
            if (indexedFiles.get(i).getObjectQualifiedPath().equals(currentFileVersion.getObjectQualifiedPath()))
                return indexedFiles.get(i);
        }
        return null;
    }
}
