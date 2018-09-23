package ru.hse.spb.javacourse.git;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;
import ru.hse.spb.javacourse.git.filestree.CommitFilesTree;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class RepositoryManager {

    public static final Path GIT_ROOT_PATH = Paths.get(".jgit/");
    public static final Path GIT_INDEX_PATH = Paths.get(".jgit/index/");
    public static final Path GIT_OBJECTS_PATH = Paths.get(".jgit/objects/");
    public static final Path GIT_REFS_PATH = Paths.get(".jgit/refs/");
    public static final Path GIT_TREES_PATH = Paths.get(".jgit/trees/");
    public static final Path GIT_FILE_STATES_PATH = Paths.get(".jgit/states");

    private static List<Blob> index;

    public static void initialize() throws RepositoryAlreadyInitializedException, IOException {
        if (isInitialized())
            throw new RepositoryAlreadyInitializedException();
        Files.createDirectory(GIT_ROOT_PATH);
        Files.createFile(GIT_INDEX_PATH);
        Files.createDirectory(GIT_OBJECTS_PATH);
        Files.createFile(GIT_REFS_PATH);
        Files.write(GIT_REFS_PATH, Collections.singletonList(new JSONArray().toString()));
        Files.createFile(GIT_FILE_STATES_PATH);
        Files.write(GIT_FILE_STATES_PATH, Collections.singletonList(new JSONArray().toString()));
    }

    @NotNull
    public static List<String> showLog(@NotNull String fromRevision) throws IOException {
        if (isRevisionNotExists(fromRevision)) {
            throw new IllegalArgumentException();
        }
        List<String> log = new ArrayList<>();
        Commit currentCommit = Commit.ofHead();
        if (currentCommit == null) {
            return log;
        }
        while (!currentCommit.getHash().equals(fromRevision)) {
            log.add(currentCommit.log());
            currentCommit = Commit.ofRevision(currentCommit.getParentHash());
        }
        log.add(currentCommit.log());
        return log;
    }

    @NotNull
    public static List<String> showLog() throws IOException {
        List<String> log = new ArrayList<>();
        Commit currentCommit = Commit.ofHead();
        if (currentCommit == null) {
            return log;
        }
        while (currentCommit.getParentHash() != null) {
            log.add(currentCommit.log());
            currentCommit = Commit.ofRevision(currentCommit.getParentHash());
        }
        log.add(currentCommit.log());
        return log;
    }

    public static void commit(@NotNull String message, @NotNull List<String> filenames) throws IOException {
        Commit.makeAndSubmit(message, filenames);
    }

    public static void checkout(@NotNull String revision) throws IOException {
        checkout(revision, false);
    }

    public static void reset(@NotNull String revision) throws IOException {
        checkout(revision, true);
    }

    private static void checkout(@NotNull String revision, boolean reset) throws IOException {
        index = readIndex();
        if (isRevisionNotExists(revision)) {
            throw new IllegalArgumentException();
        }
        moveToRevision(revision, reset);
    }

    private static void moveToRevision(@NotNull String revision, boolean deleteNewerChanges) throws IOException {
        List<String> revisionsForDeletion = new ArrayList<>();
        while (true) {
            String currentHeadHash = Commit.ofHead().getHash();
            if (currentHeadHash.equals(revision))
                break;
            revertLastCommit();
            revisionsForDeletion.add(currentHeadHash);
        }
        rewriteIndex();
        if (deleteNewerChanges) {
            deleteObjectsCorrespondingTo(revisionsForDeletion);
            deleteTreesCorrespondingTo(revisionsForDeletion);
        }
    }

    private static void revertLastCommit() throws IOException {
        Commit currentHead = Commit.ofHead();
        List<Blob> committedFiles = CommitFilesTree.getAllCommittedFiles(GIT_TREES_PATH.resolve(currentHead.getHash()));
        for (Blob nextCommittedFile: committedFiles) {
            Blob previousVersion = getPreviousFileVersion(currentHead, nextCommittedFile);
            if (previousVersion == null) {
                Files.delete(nextCommittedFile.getObjectQualifiedPath());
            } else {
                Files.write(previousVersion.getObjectQualifiedPath(), Collections.singletonList(previousVersion.decodeContents()));
            }
        }
        if (currentHead.getParentHash() != null) {
            Commit.ofRevision(currentHead.getParentHash()).setAsHead();
        }
        updateIndex(committedFiles.size());
    }

    private static void updateIndex(int filesChangedByRevertedCommit) {
        index = index.subList(0, index.size() - filesChangedByRevertedCommit);
    }

    @NotNull
    private static List<Blob> readIndex() throws IOException {
        List<Blob> blobsInIndex = new ArrayList<>();
        for (JSONObject nextBlobIndex : Files.lines(GIT_INDEX_PATH).map(JSONObject::new).collect(Collectors.toList())) {
            blobsInIndex.add(new Blob(nextBlobIndex));
        }
        return blobsInIndex;
    }

    @Nullable
    private static Blob getPreviousFileVersion(@NotNull Commit latestCommit, @NotNull Blob currentFileVersion) {
        for (int i = index.size() - latestCommit.getNumberOfCommittedFiles() - 1; i >= 0; i--) {
            if (index.get(i).getObjectQualifiedPath().equals(currentFileVersion.getObjectQualifiedPath()))
                return index.get(i);
        }
        return null;
    }

    private static boolean isInitialized() {
        return Files.exists(GIT_ROOT_PATH);
    }

    private static void rewriteIndex() throws IOException {
        Files.write(
                GIT_INDEX_PATH,
                index.stream().map(Blob::toJson).map(JSONObject::toString).collect(Collectors.toList())
        );
    }

    private static void deleteTreesCorrespondingTo(List<String> revisionsForDeletion) throws IOException {
        for (String revision: revisionsForDeletion) {
            Path pathForDeletion = GIT_TREES_PATH.resolve(revision);
            deleteDirectory(pathForDeletion);
        }
    }

    private static void deleteObjectsCorrespondingTo(List<String> revisionsForDeletion) throws IOException {
        for (String revision: revisionsForDeletion) {
            Path pathForDeletion = GIT_OBJECTS_PATH.resolve(revision.substring(0, 2)).resolve(revision.substring(2));
            Files.delete(pathForDeletion);
            while (pathForDeletion.getParent() != null && Files.list(pathForDeletion.getParent()).count() == 0) {
                pathForDeletion = pathForDeletion.getParent();
                Files.delete(pathForDeletion);
            }
        }
    }

    private static boolean isRevisionNotExists(@NotNull String revision) {
        return !Files.exists(GIT_TREES_PATH.resolve(revision));
    }

    private static void deleteDirectory(Path path) throws IOException {
        Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }
}
