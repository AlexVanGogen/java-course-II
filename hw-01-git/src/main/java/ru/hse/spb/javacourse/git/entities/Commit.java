package ru.hse.spb.javacourse.git.entities;

import org.apache.commons.codec.digest.DigestUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;
import ru.hse.spb.javacourse.git.FileUtils;
import ru.hse.spb.javacourse.git.filestatus.FileStatus;
import ru.hse.spb.javacourse.git.filestatus.StatusChecker;
import ru.hse.spb.javacourse.git.filestree.CommitFilesTree;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ru.hse.spb.javacourse.git.entities.RepositoryManager.*;

public class Commit {

    private static final Path GIT_COMMITS_PATH = Paths.get(".jgit/objects/");

    private String hash = null;
    private String hashPrefix;
    private String hashSuffix;
    private String parentHash;
    private CommitFilesTree filesTree;
    private String message;
    private long timestamp;
    private List<Path> committingFiles;

    /* First parent is always the parent that was assigned to commit after submitting;
       second parent (if presented) refers to branch that was merged to the branch
       the first parent refers to ("original branch").
       Since we do not support `octopus` merge strategy, a commit can not have more than
       two parents.
     */
    private List<String> parentHashes = new ArrayList<>();

    private Commit(@NotNull String message, @NotNull List<String> committingFileNames) {
        this.message = message;
        committingFiles = new ArrayList<>();
        for (String nextCommittingFileName : committingFileNames) {
            committingFiles.add(Paths.get(nextCommittingFileName));
        }
    }

    private Commit(@NotNull String hash, long timestamp, @NotNull String message, @NotNull List<String> parentHashes) throws IOException {
        this.hash = hash;
        this.hashPrefix = hash.substring(0, 2);
        this.hashSuffix = hash.substring(2);
        this.timestamp = timestamp;
        this.message = message;
        this.parentHashes = parentHashes;
//        this.parentHash = parentHash;
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
                commitData.has("parents")
                        ? commitData.getJSONArray("parents").toList()
                            .stream()
                            .map(o -> (String) o)
                            .collect(Collectors.toList())
                        : Collections.emptyList()
        );
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
            throw new RevisionNotFoundException(revision);
        }
        Stream<String> commitInfoLines = Files.lines(revisionPath);
        JSONObject commitData = new JSONObject(commitInfoLines.collect(Collectors.joining(",")));
        return new Commit(revision, commitData);
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
        statusChecker.getActualFileStates();
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

    public String log() {
        return hash + " [" + message + "] " + new Date(timestamp * 1000);
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
            nextChangedFile.save();
        }
    }

    public String getHash() {
        return hash;
    }

    public String getParentHash(int i) {
        return parentHashes.get(i);
    }

    public List<String> getParentsHashes() {
        return parentHashes;
    }

    public boolean hasParents() {
        return !parentHashes.isEmpty();
    }

    public String getMessage() {
        return message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    private void submit(boolean saveBlob) throws IOException {
//        parentHash = getHead();
        final String hashOfHead = getHead();
        if (hashOfHead != null) {
            parentHashes.add(hashOfHead);
        }
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

    public JSONObject toJson() {
        JSONObject commitData = new JSONObject();
        commitData.put("tree", filesTree.getHash());
        if (!parentHashes.isEmpty()) {
            JSONArray parentsJSON = new JSONArray(parentHashes);
            commitData.put("parents", parentsJSON);
        }
        commitData.put("timestamp", timestamp);
        commitData.put("message", message);
        return commitData;
    }

    private void updateCommitTree() throws IOException {
        if (Files.notExists(GIT_COMMITS_PATH)) {
            Files.createDirectory(GIT_COMMITS_PATH);
        }
        Path prefixedSubdirectory = GIT_COMMITS_PATH.resolve(hashPrefix);
        if (Files.notExists(prefixedSubdirectory)) {
            Files.createDirectory(prefixedSubdirectory);
        }
        Path commitPath = Files.createFile(prefixedSubdirectory.resolve(hashSuffix));
        Files.write(commitPath, Collections.singletonList(toJson().toString()));
    }

    private static String getHead() throws IOException {
        RefList refList = new RefList();
        return refList.getRevisionReferencedFromHead();
    }

    private void updateRefs() throws IOException {
        RefList refList = new RefList();
        refList.update(HEAD_REF_NAME, currentBranch);
        refList.update(currentBranch, hash);
        refList.write();
    }
}
