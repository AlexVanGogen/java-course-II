package ru.hse.spb.javacourse.git.entities;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static ru.hse.spb.javacourse.git.FileUtils.deleteDirectory;
import static ru.hse.spb.javacourse.git.entities.RepositoryManager.GIT_INDEX_PATH;
import static ru.hse.spb.javacourse.git.entities.RepositoryManager.GIT_OBJECTS_PATH;
import static ru.hse.spb.javacourse.git.entities.RepositoryManager.GIT_TREES_PATH;

public class Index {

    @NotNull private List<Blob> indexedFiles;

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

    public void removeBlobIfExists(@NotNull Blob blob, boolean removeAllReferences) throws IOException {
        if (!indexedFiles.contains(blob)) {
            return;
        }
        indexedFiles.remove(blob);
        if (removeAllReferences) {
            Path pathToCorrespondingObject = blob.getObjectCorrespondingToBlob();
            if (pathToCorrespondingObject != null) {
                deleteDirectory(pathToCorrespondingObject.getParent());
                removeBlobFromTrees(blob);
            }
        }
    }

    public void removeBlobsWithPath(@NotNull String pathToFile, boolean removeAllReferences) throws IOException {
        for (Blob blobToRemove : indexedFiles.stream()
                .filter(b -> b.getObjectQualifiedPath().toString().equals(pathToFile))
                .collect(Collectors.toList())) {
            removeBlobIfExists(blobToRemove, removeAllReferences);
        }
    }

    public void updateIndex(int numberOfFilesChangedByRevertedCommit) {
        if (indexedFiles.size() <= numberOfFilesChangedByRevertedCommit) {
            indexedFiles.clear();
        } else {
            indexedFiles = indexedFiles.subList(0, indexedFiles.size() - numberOfFilesChangedByRevertedCommit);
        }
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
    public Blob findPreviousVersionOfFile(@NotNull Blob currentFileVersion) {
        int versionsVisited = 0;
        for (int i = indexedFiles.size() - 1; i >= 0; i--) {
            if (indexedFiles.get(i).getObjectQualifiedPath().equals(currentFileVersion.getObjectQualifiedPath())) {
                if (versionsVisited == 1) {
                    return indexedFiles.get(i);
                }
                versionsVisited++;
            }
        }
        return null;
    }

    @NotNull public List<Blob> getIndexedFiles() {
        return indexedFiles;
    }

    private void removeBlobFromTrees(@NotNull Blob blob) throws IOException {
        for (Path path : Files.walk(GIT_TREES_PATH)
                .filter(path -> path.endsWith(blob.getObjectQualifiedPath()))
                .collect(Collectors.toList())) {
            Files.deleteIfExists(path);
        }
    }
}
