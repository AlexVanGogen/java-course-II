package ru.hse.spb.javacourse.git.filestatus;

import org.apache.commons.codec.binary.Base64;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import ru.hse.spb.javacourse.git.Blob;
import ru.hse.spb.javacourse.git.Commit;
import ru.hse.spb.javacourse.git.filestree.CommitFilesTree;

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

    public void updateFileStates() throws IOException {
        collectCurrentFileStates();
        Commit lastCommit = Commit.ofHead();
        List<Blob> committedFiles;
        if (lastCommit != null) {
            committedFiles = CommitFilesTree.getAllCommittedFiles(GIT_TREES_PATH.resolve(lastCommit.getHash()));
            while (lastCommit.getParentHash() != null) {
                lastCommit = Commit.ofRevision(lastCommit.getParentHash());
                committedFiles.addAll(CommitFilesTree.getAllCommittedFiles(GIT_TREES_PATH.resolve(lastCommit.getHash())));
            }
        } else {
            committedFiles = new ArrayList<>();
        }

        JSONArray fileStatesData = new JSONArray();
        List<Path> filePaths = Files.walk(ROOT_PATH)
                .filter(path -> !path.startsWith(GIT_ROOT_PATH))
                .filter(Files::isRegularFile)
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
                    state = FileStatus.MODIFIED;
                }
                committedFiles.remove(correspondingBlob.get());
            } else {
                state = FileStatus.MODIFIED;
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

    private boolean contentsEqual(@NotNull Path pathToFile, @NotNull Blob blob) throws IOException {
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
