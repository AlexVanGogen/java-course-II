package ru.hse.spb.javacourse.git.entities;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import ru.hse.spb.javacourse.git.filestree.CommitFilesTree;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static ru.hse.spb.javacourse.git.FileUtils.deleteDirectory;

public class RepositoryManager {

    public static final Path GIT_ROOT_PATH = Paths.get(".jgit/");
    public static final Path GIT_INDEX_PATH = Paths.get(".jgit/index/");
    public static final Path GIT_OBJECTS_PATH = Paths.get(".jgit/objects/");
    public static final Path GIT_REFS_PATH = Paths.get(".jgit/refs/");
    public static final Path GIT_TREES_PATH = Paths.get(".jgit/trees/");
    public static final Path GIT_FILE_STATES_PATH = Paths.get(".jgit/states");
    public static final Path GIT_STAGE_PATH = Paths.get(".jgit/stage");
    public static final Path GIT_MERGING_BRANCH_PATH = Paths.get(".jgit/merge");
    public static final Path GIT_FILES_WITH_CONFLICTS_PATH = Paths.get(".jgit/conflicts");

    private static Index index;

    public static final String HEAD_REF_NAME = "HEAD";
    public static final String DEFAULT_BRANCH_NAME = "master";

    public static String currentBranch = DEFAULT_BRANCH_NAME;

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
        Files.createFile(GIT_STAGE_PATH);
        Files.write(GIT_STAGE_PATH, Collections.singletonList(new JSONArray().toString()));
        Files.createFile(GIT_MERGING_BRANCH_PATH);
        Files.createFile(GIT_FILES_WITH_CONFLICTS_PATH);
    }

    @NotNull
    public static List<String> showLog(@NotNull String fromRevision) throws IOException {
        if (isRevisionNotExists(fromRevision)) {
            throw new RevisionNotFoundException(fromRevision);
        }
        List<String> log = new ArrayList<>();
        RefList refList = new RefList();
        List<Commit> commitsList = getCommitsUptoRevision(HEAD_REF_NAME, CheckoutKind.BRANCH);
        for (final Commit nextCommit : commitsList) {
            if (nextCommit.getHash().equals(fromRevision)) {
                log.add(withRefs(nextCommit.log(), nextCommit.getHash(), refList));
                break;
            }
            log.add(withRefs(nextCommit.log(), nextCommit.getHash(), refList));
        }
        return log;
    }

    @NotNull
    public static List<String> showLog() throws IOException {
        List<String> log = new ArrayList<>();
        RefList refList = new RefList();
        getCommitsUptoRevision(HEAD_REF_NAME, CheckoutKind.REVISION).forEach(commit -> {
            log.add(withRefs(commit.log(), commit.getHash(), refList));
        });
        return log;
    }

    public static void commit(@NotNull String message, @NotNull List<String> filenames) throws IOException, NothingToCommitException {
        Commit.makeAndSubmit(message, filenames);
    }

    @NotNull
    public static CheckoutKind checkout(@NotNull String revisionOrPointer) throws IOException {
        final CheckoutKind kind = checkout(revisionOrPointer, false);
        if (kind == CheckoutKind.BRANCH) {
            currentBranch = revisionOrPointer;
        }
        index.writeIndex();
//        new StatusChecker().getActualFileStates();
        return kind;
    }

    public static void reset(@NotNull String revision) throws IOException {
        checkout(revision, true);
//        new StatusChecker().getActualFileStates();
    }

    @NotNull
    public static List<Commit> getCommitsUptoRevision(@NotNull String revisionOrPointer, @NotNull CheckoutKind kind) throws IOException {
        RefList refList = new RefList();
        List<Commit> commits = new ArrayList<>();
        Commit currentCommit;
        if (kind == CheckoutKind.BRANCH) {
            if (!revisionOrPointer.equals(HEAD_REF_NAME)) {
                final String revisionForRef = refList.getRevisionForRef(revisionOrPointer);
                if (revisionForRef == null) {
                    throw new RevisionNotFoundException();
                }
                currentCommit = Commit.ofRevision(revisionForRef);
            } else {
                currentCommit = Commit.ofHead();
            }
        } else {
            if (revisionOrPointer.equals(HEAD_REF_NAME)) {
                currentCommit = Commit.ofHead();
            } else {
                currentCommit = Commit.ofRevision(revisionOrPointer);
            }
        }
        if (currentCommit != null) {
            while (currentCommit.hasParents()) {
                commits.add(currentCommit);
                currentCommit = Commit.ofRevision(currentCommit.getParentHash(0));
            }
            commits.add(currentCommit);
        }
        return commits;
    }

    @NotNull
    private static String withRefs(@NotNull String basicLog, @NotNull String revision, @NotNull RefList refList) {
        List<String> refsToRevision = refList.getRefsToRevision(revision);
        if (!refsToRevision.isEmpty()) {
            basicLog += " <- " + String.join(", ", refsToRevision);
        }
        return basicLog;
    }

    public static void deleteRevisionHistory(@NotNull String revision) throws IOException {
        deleteTreeCorrespondingTo(revision);
        deleteObjectCorrespondingTo(revision);
    }

    @NotNull
    private static CheckoutKind checkout(@NotNull String revisionOrPointer, boolean reset) throws IOException {
        index = Index.getIndex();
        RefList refList = new RefList();
        CheckoutKind kind;
        if (isRevisionNotExists(revisionOrPointer)) {
            if (!refList.hasBranchWithName(revisionOrPointer)) {
                throw new RevisionNotFoundException(revisionOrPointer);
            }
            kind = CheckoutKind.BRANCH;
        } else {
            kind = CheckoutKind.REVISION;
        }
        if (reset) {
            moveToRevision(revisionOrPointer, true);
            return kind;
        }
        final List<String> revisionsOfCurrentBranch = getCommitsUptoRevision(HEAD_REF_NAME, kind).stream().map(Commit::getHash).collect(Collectors.toList());
        final List<Commit> commitsOfTargetBranch = getCommitsUptoRevision(revisionOrPointer, kind);
        Commit latestCommonCommit = null;
        List<Commit> commitsToRestore = new ArrayList<>();
        for (final Commit earlierCommit : commitsOfTargetBranch) {
            if (revisionsOfCurrentBranch.contains(earlierCommit.getHash())) {
                latestCommonCommit = earlierCommit;
                break;
            }
            commitsToRestore.add(earlierCommit);
        }
        if (latestCommonCommit != null) {
            moveToRevision(latestCommonCommit.getHash(), reset);
            Collections.reverse(commitsToRestore);
            for (final Commit earlierCommit : commitsToRestore) {
                earlierCommit.restoreChanges();
            }
            index = Index.getIndex();
            refList.update(HEAD_REF_NAME, revisionOrPointer);
            refList.write();
        }
        return kind;
    }

    private static void moveToRevision(@NotNull String revision, boolean deleteNewerChanges) throws IOException {
        List<String> revisionsForDeletion = new ArrayList<>();
        Path currentPath = Paths.get("");

        List<String> filePaths = Files.walk(currentPath)
                .filter(path -> !path.startsWith(GIT_ROOT_PATH))
                .filter(Files::isRegularFile)
                .filter(file -> file.toString().endsWith(".txt"))
                .map(Path::toString)
                .collect(Collectors.toList());
        FileUnstager unstager = new FileUnstager(filePaths);
        unstager.unstage();
        index = unstager.getIndex();
        index.writeIndex();

        RefList refList = new RefList();
        while (true) {
            String currentHeadHash = Commit.ofHead().getHash();
            if (currentHeadHash.equals(revision))
                break;
            revertLastCommit();
            final List<String> refsToHeadRevision = refList.getRefsToRevision(currentHeadHash);
            refsToHeadRevision.remove(HEAD_REF_NAME);
            refsToHeadRevision.remove(currentBranch);
            if (refsToHeadRevision.isEmpty()) {
                revisionsForDeletion.add(currentHeadHash);
            }
        }
        index.writeIndex();
        if (deleteNewerChanges) {
            deleteObjectsCorrespondingTo(revisionsForDeletion);
            deleteTreesCorrespondingTo(revisionsForDeletion);
        }
    }

    private static void revertCommit(@NotNull String revision) throws IOException {
        List<Blob> committedFiles = CommitFilesTree.getAllCommittedFiles(GIT_TREES_PATH.resolve(revision));
        for (Blob nextCommittedFile: committedFiles) {
            Blob previousVersion = index.findPreviousVersionOfFile(nextCommittedFile);
            if (previousVersion == null) {
                Files.deleteIfExists(nextCommittedFile.getObjectQualifiedPath());
            } else {
                Files.write(previousVersion.getObjectQualifiedPath(), Collections.singletonList(previousVersion.decodeContents()));
            }
        }
        RefList refList = new RefList();
        final Commit commit = Commit.ofRevision(revision);
        if (commit.hasParents()) {
            final String parentHash = commit.getParentHash(0);
            refList.update(currentBranch, parentHash);
        }
        refList.write();
        index.updateIndex(committedFiles.size());
    }

    private static void revertLastCommit() throws IOException {
        Commit currentHead = Commit.ofHead();
        revertCommit(currentHead.getHash());
    }

    private static boolean isInitialized() {
        return Files.exists(GIT_ROOT_PATH);
    }

    private static void deleteTreesCorrespondingTo(List<String> revisionsForDeletion) throws IOException {
        for (String revision: revisionsForDeletion) {
            deleteTreeCorrespondingTo(revision);
        }
    }

    private static void deleteTreeCorrespondingTo(String revision) throws IOException {
        Path pathForDeletion = GIT_TREES_PATH.resolve(revision);
        deleteDirectory(pathForDeletion);
    }

    private static void deleteObjectsCorrespondingTo(List<String> revisionsForDeletion) throws IOException {
        for (String revision: revisionsForDeletion) {
            deleteObjectCorrespondingTo(revision);
        }
    }

    private static void deleteObjectCorrespondingTo(String revision) throws IOException {
        Path pathForDeletion = GIT_OBJECTS_PATH.resolve(revision.substring(0, 2)).resolve(revision.substring(2));
        Files.delete(pathForDeletion);
        while (pathForDeletion.getParent() != null && Files.list(pathForDeletion.getParent()).count() == 0) {
            pathForDeletion = pathForDeletion.getParent();
            Files.delete(pathForDeletion);
        }
    }

    private static boolean isRevisionNotExists(@NotNull String revision) {
        return !Files.exists(GIT_TREES_PATH.resolve(revision));
    }
}
