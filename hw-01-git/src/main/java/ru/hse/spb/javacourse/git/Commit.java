package ru.hse.spb.javacourse.git;

import org.apache.commons.codec.digest.DigestUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;
import ru.hse.spb.javacourse.git.command.Add;
import ru.hse.spb.javacourse.git.filestatus.FileStatus;
import ru.hse.spb.javacourse.git.filestatus.StatusChecker;
import ru.hse.spb.javacourse.git.filestree.CommitFilesTree;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Commit {

    private static final Path GIT_COMMITS_PATH = Paths.get(".jgit/objects/");
    private static final Path GIT_REFS_PATH = Paths.get(".jgit/refs/");
    private static final Path GIT_TREES_PATH = Paths.get(".jgit/trees/");

    private String hash;
    private String hashPrefix;
    private String hashSuffix;
    private String parentHash;
    private CommitFilesTree filesTree;
    private String message;
    private long timestamp;
    private List<Path> committingFiles;

    private Commit(@NotNull String message, @NotNull List<String> committingFileNames) {
        this.message = message;
        committingFiles = new ArrayList<>();
        for (String nextCommittingFileName : committingFileNames) {
            committingFiles.add(Paths.get(nextCommittingFileName));
        }
    }

    private Commit(@NotNull String hash, long timestamp, @NotNull String message, @Nullable String parentHash) throws IOException {
        this.hash = hash;
        this.hashPrefix = hash.substring(0, 2);
        this.hashSuffix = hash.substring(2);
        this.timestamp = timestamp;
        this.message = message;
        this.parentHash = parentHash;
        this.committingFiles = Files.walk(GIT_TREES_PATH.resolve(hash))
                .filter(FileUtils::isTextFile)
                .collect(Collectors.toList());
    }

    private Commit(@NotNull String hash, long timestamp, @NotNull String message) throws IOException {
        this(hash, timestamp, message, null);
    }

    public Commit(@NotNull String hash, @NotNull JSONObject commitData) throws IOException {
        this(
                hash,
                commitData.getLong("timestamp"),
                commitData.getString("message"),
                commitData.has("parent") ? commitData.getString("parent") : null
        );
    }

    public static void makeAndSubmit(@NotNull String message, @NotNull List<String> committingFileNames) throws IOException, NothingToCommitException {
        StatusChecker statusChecker = new StatusChecker();
        statusChecker.getActualFileStates();
        List<String> filesThatWillBeCommitted = new ArrayList<>();
        for (String committingFile: committingFileNames) {
            FileStatus fileStatus = statusChecker.getState(committingFile);
            if (fileStatus.equals(FileStatus.STAGED) || fileStatus.equals(FileStatus.MODIFIED)) {
                filesThatWillBeCommitted.add(committingFile);
            }
        }
        if (filesThatWillBeCommitted.isEmpty()) {
            throw new NothingToCommitException();
        }
        Commit commit = new Commit(message, filesThatWillBeCommitted);
        commit.submit(true);
        Stage stage = Stage.getStage();
        filesThatWillBeCommitted.forEach(stage::removeFromStageIfExists);
        stage.writeStage();
    }

    public static void makeAndSubmitStaged(@NotNull String message, @NotNull Stage stage) throws IOException, NothingToCommitException {
        List<String> stagedFilePaths = stage.getStagedFilePaths();
        if (stagedFilePaths.isEmpty()) {
            throw new NothingToCommitException();
        }
        Commit commit = new Commit(message, stagedFilePaths);
        commit.submit(false);
        stage.resetStage();
    }

    @Nullable
    public static Commit ofHead() throws IOException {
        String headHash = getHead();
        if (headHash == null) {
            return null;
        }
        return ofRevision(headHash);
    }

    @NotNull
    public static Commit ofRevision(@NotNull String revision) throws IOException {
        Path revisionPath = GIT_COMMITS_PATH.resolve(revision.substring(0, 2)).resolve(revision.substring(2));
        if (Files.notExists(revisionPath)) {
            throw new RevisionNotFoundException();
        }
        Stream<String> commitInfoLines = Files.lines(revisionPath);
        JSONObject commitData = new JSONObject(commitInfoLines.collect(Collectors.joining(",")));
        return new Commit(revision, commitData);
    }

    public String log() throws IOException {
        String headHash = getHead();
        return hash + " [" + message + "] " + new Date(timestamp * 1000) + (hash.equals(headHash) ? " <- HEAD" : "");
    }

    public int getNumberOfCommittedFiles() {
        return committingFiles.size();
    }

    public void restoreChanges() throws IOException {
        Path commitChangesTreePath = GIT_TREES_PATH.resolve(hash);
        List<Blob> changedFiles = CommitFilesTree.getAllCommittedFiles(commitChangesTreePath);
        for (Blob nextChangedFile: changedFiles) {
            Path fileToRestorePath = Paths.get("").resolve(nextChangedFile.getObjectQualifiedPath());
            if (Files.notExists(fileToRestorePath)) {
                Files.createFile(fileToRestorePath);
            }
            Files.write(fileToRestorePath, Collections.singletonList(nextChangedFile.decodeContents()));
        }
    }

    public String getHash() {
        return hash;
    }

    public String getParentHash() {
        return parentHash;
    }

    public String getMessage() {
        return message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    private void submit(boolean saveBlob) throws IOException {
        parentHash = getHead();
        timestamp = Instant.now().getEpochSecond();
        hash = DigestUtils.sha1Hex(String.valueOf(timestamp) + message);
        hashPrefix = hash.substring(0, 2);
        hashSuffix = hash.substring(2);
        filesTree = new CommitFilesTree(hash);
        filesTree.saveFiles(committingFiles);
        filesTree.write(saveBlob);
        updateCommitTree();
        updateRefs();
    }

    public void setAsHead() throws IOException {
        updateRefs();
    }

    public JSONObject toJson() {
        JSONObject commitData = new JSONObject();
        commitData.put("tree", filesTree.getHash());
        if (parentHash != null) {
            commitData.put("parent", parentHash);
        }
        commitData.put("timestamp", timestamp);
        commitData.put("message", message);
        return commitData;
    }

    private void updateCommitTree() throws IOException {
        if (Files.notExists(GIT_COMMITS_PATH)) {
            Files.createDirectory(GIT_COMMITS_PATH);
        }
        Path commitPath = Files.createFile(Files.createDirectory(GIT_COMMITS_PATH.resolve(hashPrefix)).resolve(hashSuffix));
        Files.write(commitPath, Collections.singletonList(toJson().toString()));
    }

    private static String getHead() throws IOException {
        JSONArray refsList = new JSONArray(Files.lines(GIT_REFS_PATH).collect(Collectors.joining(",")));
        for (Object nextReference: refsList) {
            JSONObject refJson = (JSONObject) nextReference;
            if (refJson.getString("name").equals("HEAD")) {
                return refJson.getString("revision");
            }
        }
        return null;
    }

    private void updateRefs() throws IOException {
        JSONArray refsList = new JSONArray(Files.lines(GIT_REFS_PATH).collect(Collectors.joining(",")));
        JSONArray updatedRefsList = new JSONArray();
        boolean isHeadFound = false;
        for (Object nextReference: refsList) {
            JSONObject refJson = (JSONObject) nextReference;
            if (refJson.getString("name").equals("HEAD")) {
                refJson.put("revision", hash);
                isHeadFound = true;
            }
            updatedRefsList.put(refJson);
        }
        if (!isHeadFound) {
            JSONObject headData = new JSONObject();
            headData.put("name", "HEAD");
            headData.put("revision", hash);
            updatedRefsList.put(headData);
        }
        Files.write(GIT_REFS_PATH, Collections.singletonList(updatedRefsList.toString()));
    }
}
