package ru.hse.spb.javacourse.git.filestatus;

import org.apache.commons.codec.binary.Base64;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;
import ru.hse.spb.javacourse.git.*;
import ru.hse.spb.javacourse.git.command.Status;
import ru.hse.spb.javacourse.git.filestree.CommitFilesTree;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static ru.hse.spb.javacourse.git.RepositoryManager.GIT_FILE_STATES_PATH;
import static ru.hse.spb.javacourse.git.RepositoryManager.GIT_ROOT_PATH;
import static ru.hse.spb.javacourse.git.RepositoryManager.GIT_TREES_PATH;

public class StatusChecker {

    private static final Path ROOT_PATH = Paths.get("");

    private Map<String, FileStatus> fileStates = new HashMap<>();
    private Index index = Index.getIndex();

    public StatusChecker() throws IOException { }

    /**
     * Give status to every file in the root directory.
     *
     * 1) A file is staged, if corresponding record exists in `stages` file
     *    and it weren't modified since staging.
     * 2) A file is unchanged, if it weren't changed comparing to
     *    latest committed version.
     * 3) A file is deleted, if it is not in the current directory, but
     *    was committed earlier.
     * 4) In other cases, a file is modified.
     *
     * @throws IOException if any I/O error occurs
     */
    private void updateFileStates() throws IOException {
        collectCurrentFileStates();
        index = Index.getIndex();
        Stage stage = Stage.getStage();
        Commit lastCommit = Commit.ofHead();
        Set<Blob> committedFiles = new TreeSet<>(Comparator.comparing(Blob::getObjectQualifiedPath));
        if (lastCommit != null) {
            committedFiles.addAll(CommitFilesTree.getAllCommittedFiles(GIT_TREES_PATH.resolve(lastCommit.getHash())));
            while (lastCommit.getParentHash() != null) {
                lastCommit = Commit.ofRevision(lastCommit.getParentHash());
                committedFiles.addAll(CommitFilesTree.getAllCommittedFiles(GIT_TREES_PATH.resolve(lastCommit.getHash())));
            }
        }

        JSONArray fileStatesData = new JSONArray();
        List<Path> filePaths = Files.walk(ROOT_PATH)
                .filter(path -> !path.startsWith(GIT_ROOT_PATH))
                .filter(Files::isRegularFile)
                .filter(file -> file.toString().endsWith(".txt"))
                .collect(Collectors.toList());

        for (Path nextPath : filePaths) {
            JSONObject fileState = new JSONObject();
            FileStatus state;
            Optional<Blob> correspondingBlob = committedFiles.stream()
                    .filter(blob -> blob.getObjectQualifiedPath().equals(nextPath))
                    .findFirst();
            if (correspondingBlob.isPresent()) {
                if (contentsEqual(nextPath, correspondingBlob.get())) {
                    state = FileStatus.UNCHANGED;
                } else {
                    state = recalculateStateForStagedFile(nextPath, stage);
                }
                committedFiles.remove(correspondingBlob.get());
            } else {
                state = recalculateStateForStagedFile(nextPath, stage);
            }
            fileState.put("path", nextPath.toString());
            fileState.put("state", state.toString());
            fileStatesData.put(fileState);
        }

        for (Blob blobOfDeletedFile: committedFiles) {
            JSONObject deletedFileState = new JSONObject();
            deletedFileState.put("path", blobOfDeletedFile.getObjectQualifiedPath().toString());
            deletedFileState.put("state", FileStatus.DELETED);
            fileStatesData.put(deletedFileState);
        }

        Files.write(GIT_FILE_STATES_PATH, Collections.singletonList(fileStatesData.toString()));
    }

    public Map<FileStatus, List<String>> getActualFileStates() throws IOException {
        updateFileStates();
        collectCurrentFileStates();
        return fileStates
                .entrySet()
                .stream()
                .collect(Collectors.groupingBy(Map.Entry::getValue, Collectors.mapping(Map.Entry<String, FileStatus>::getKey, Collectors.toList())));
    }

    @NotNull public FileStatus getState(@NotNull String pathAsString) {
        if (!fileStates.containsKey(pathAsString)) {
            throw new FileHasNoStatusException(pathAsString);
        }
        return fileStates.get(pathAsString);
    }

    private FileStatus recalculateStateForStagedFile(@NotNull Path pathToFile, @NotNull Stage stage) throws IOException {
        FileStatus state = stage.fileInStage(pathToFile.toString())
                && contentsEqual(pathToFile, index.findLatestVersionOfFile(pathToFile)) ? FileStatus.STAGED : FileStatus.MODIFIED;
        if (!state.equals(FileStatus.STAGED)) {
            stage.removeFromStageIfExists(pathToFile.toString());
        }
        stage.writeStage();
        return state;
    }

    private boolean contentsEqual(@NotNull Path pathToFile, @Nullable Blob blob) throws IOException {
        if (blob == null) {
            return false;
        }
        String encodedFileContents = Base64.encodeBase64String(String.join("\n", Files.readAllLines(pathToFile)).getBytes());
        return encodedFileContents.equals(blob.getEncodedContents());
    }

    private void collectCurrentFileStates() throws IOException {
        fileStates.clear();
        JSONArray fileStatesData = new JSONArray(String.join("\n", Files.readAllLines(GIT_FILE_STATES_PATH)));
        for (Object nextFileState: fileStatesData) {
            JSONObject fileState = (JSONObject) nextFileState;
            fileStates.put(fileState.getString("path"), FileStatus.valueOf(fileState.getString("state")));
        }
    }
}
