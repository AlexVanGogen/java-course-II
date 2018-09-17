package ru.hse.spb.javacourse.git;

import com.sun.org.apache.xml.internal.security.exceptions.Base64DecodingException;
import org.apache.commons.codec.digest.DigestUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

    public static void makeAndSubmit(@NotNull String message, @NotNull List<String> committingFileNames) throws IOException {
        Commit commit = new Commit(message, committingFileNames);
        commit.submit();
    }

    public static Commit ofHead() throws IOException {
        String headHash = getHead();
        if (headHash == null) {
            throw new RevisionNotFoundException();
        }
        return ofRevision(headHash);
    }

    public static Commit ofRevision(@NotNull String revision) throws IOException {
        Path revisionPath = GIT_COMMITS_PATH.resolve(revision.substring(0, 2)).resolve(revision.substring(2));
        if (Files.notExists(revisionPath)) {
            throw new RevisionNotFoundException();
        }
        List<String> commitInfoLines = Files.readAllLines(revisionPath);
        if (commitInfoLines.size() == 3) {
            return new Commit(revision, Long.valueOf(commitInfoLines.get(1)), commitInfoLines.get(2));
        } else if (commitInfoLines.size() == 4) {
            return new Commit(revision, Long.valueOf(commitInfoLines.get(2)), commitInfoLines.get(3), commitInfoLines.get(1).split(" ")[1]);
        } else throw new IncorrectStorageFormatException();
    }

    public String log() throws IOException {
        String headHash = getHead();
        return hash + " [" + message + "] " + new Date(timestamp * 1000) + (hash.equals(headHash) ? " <- HEAD" : "");
    }

    public List<Path> getCommittedFiles() {
        return committingFiles;
    }

    public int getNumberOfCommittedFiles() {
        return committingFiles.size();
    }

    public void restoreChanges() throws IOException, Base64DecodingException {
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

    private void submit() throws IOException {
        parentHash = getHead();
        timestamp = Instant.now().getEpochSecond();
        hash = DigestUtils.sha1Hex(String.valueOf(timestamp) + message);
        hashPrefix = hash.substring(0, 2);
        hashSuffix = hash.substring(2);
        filesTree = new CommitFilesTree(hash);
        filesTree.saveFiles(committingFiles);
        filesTree.write();
        updateCommitTree();
        updateRefs();
    }

    public void setAsHead() throws IOException {
        updateRefs();
    }

    private void updateCommitTree() throws IOException {
        if (Files.notExists(GIT_COMMITS_PATH)) {
            Files.createDirectory(GIT_COMMITS_PATH);
        }
        Path commitPath = Files.createFile(Files.createDirectory(GIT_COMMITS_PATH.resolve(hashPrefix)).resolve(hashSuffix));
        Files.write(commitPath, Collections.singletonList(getCommitInfo()));
    }

    private String getCommitInfo() {
        StringBuilder builder = new StringBuilder();
        builder.append("tree ").append(filesTree.getHash()).append("\n");
        if (parentHash != null) {
            builder.append("parent ").append(parentHash).append("\n");
        }
        builder.append(timestamp).append("\n").append(message);
        return builder.toString();
    }

    private static String getHead() throws IOException {
        for (String ref : Files.readAllLines(GIT_REFS_PATH)) {
            String[] refInfo = ref.split(" ");
            if (isRefForHead(refInfo)) {
                return refInfo[1];
            }
        }
        return null;
    }

    private void updateRefs() throws IOException {
        List<String> refs = Files.readAllLines(GIT_REFS_PATH);
        for (int i = 0; i < refs.size(); i++) {
            String[] refInfo = refs.get(i).split(" ");
            if (isRefForHead(refInfo)) {
                refs.set(i, refInfo[0] + " " + hash);
                Files.write(GIT_REFS_PATH, refs);
                return;
            }
        }
        Files.write(GIT_REFS_PATH, Collections.singletonList("HEAD " + hash));
    }

    private static boolean isRefForHead(String[] refInfo) {
        return (refInfo.length == 2 && refInfo[0].equals("HEAD"));
    }
}
