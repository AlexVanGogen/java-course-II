package ru.hse.spb.javacourse.git;

import com.sun.org.apache.xml.internal.security.exceptions.Base64DecodingException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

    private static final Path GIT_ROOT_PATH = Paths.get(".jgit/");
    private static final Path GIT_INDEX_PATH = Paths.get(".jgit/index/");
    private static final Path GIT_OBJECTS_PATH = Paths.get(".jgit/objects/");
    private static final Path GIT_REFS_PATH = Paths.get(".jgit/refs/");
    private static final Path GIT_TREES_PATH = Paths.get(".jgit/trees/");

    private static List<Blob> index;

    public static void initialize() throws RepositoryAlreadyInitializedException, IOException {
        if (isInitialized())
            throw new RepositoryAlreadyInitializedException();
        Files.createDirectory(GIT_ROOT_PATH);
        Files.createFile(GIT_INDEX_PATH);
        Files.createDirectory(GIT_OBJECTS_PATH);
        Files.createFile(GIT_REFS_PATH);
        System.out.println("Jgit initialized successfully");
    }

    public static void showLog(@NotNull String fromRevision) throws IOException {
        if (isRevisionNotExists(fromRevision)) {
            throw new IllegalArgumentException();
        }
        Commit currentCommit = Commit.ofHead();
        while (!currentCommit.getHash().equals(fromRevision)) {
            System.out.println(currentCommit.log());
            currentCommit = Commit.ofRevision(currentCommit.getParentHash());
        }
        System.out.println(currentCommit.log());
    }

    public static void showLog() throws IOException {
        Commit currentCommit = Commit.ofHead();
        while (currentCommit.getParentHash() != null) {
            System.out.println(currentCommit.log());
            currentCommit = Commit.ofRevision(currentCommit.getParentHash());
        }
        System.out.println(currentCommit.log());
    }

    public static void commit(@NotNull String message, @NotNull List<String> filenames) throws IOException {
        Commit.makeAndSubmit(message, filenames);
        System.out.printf("Committed %d files\n", filenames.size());
    }

    public static void checkout(@NotNull String revision) throws IOException, Base64DecodingException {
        checkout(revision, false);
    }

    public static void reset(@NotNull String revision) throws IOException, Base64DecodingException {
        checkout(revision, true);
    }

    private static void checkout(@NotNull String revision, boolean reset) throws IOException, Base64DecodingException {
        index = readIndex();
        if (isRevisionNotExists(revision)) {
            throw new IllegalArgumentException();
        }
        moveToRevision(revision, reset);
        System.out.println("Checkout to revision " + revision);
    }

    private static void moveToRevision(@NotNull String revision, boolean deleteNewerChanges) throws IOException, Base64DecodingException {
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

    private static void revertLastCommit() throws IOException, Base64DecodingException {
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

    private static List<Blob> readIndex() throws IOException {
        List<Blob> blobsInIndex = new ArrayList<>();
        for (String nextBlobDeclarationInIndex: Files.readAllLines(GIT_INDEX_PATH)) {
            String[] declarationInfo = nextBlobDeclarationInIndex.split(" ");
            blobsInIndex.add(new Blob(declarationInfo[0], declarationInfo[1]));
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
                index.stream().map(blob -> blob.getObjectQualifiedPath() + " " + blob.getSha1()).collect(Collectors.toList())
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
