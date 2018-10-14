package ru.hse.spb.javacourse.git.command;

import org.jetbrains.annotations.NotNull;
import ru.hse.spb.javacourse.git.entities.*;
import ru.hse.spb.javacourse.git.entities.Commit;
import ru.hse.spb.javacourse.git.filestatus.FileStatus;
import ru.hse.spb.javacourse.git.filestatus.StatusChecker;
import ru.hse.spb.javacourse.git.filestree.CommitFilesTree;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static ru.hse.spb.javacourse.git.entities.RepositoryManager.GIT_TREES_PATH;
import static ru.hse.spb.javacourse.git.entities.RepositoryManager.HEAD_REF_NAME;
import static ru.hse.spb.javacourse.git.entities.RepositoryManager.currentBranch;

public class Merge extends GitCommand {

    @NotNull private RefList refList;

    public Merge() throws IOException {
        refList = new RefList();
    }

    @Override
    public String execute(List<String> args) throws IOException, GitCommandException {
        if (args.size() != 1) {
            throw new GitCommandException();
        }
        String branchToMerge = args.get(0);
        return merge(branchToMerge);
    }

    @NotNull
    private String merge(@NotNull String branch) throws IOException {
        final String branchReference = refList.getRevisionForRef(branch);

        if (branchReference == null) {
            throw new GitCommandException(String.format("Branch not found: %s", branch));
        }

        StatusChecker checker = new StatusChecker();
        final Map<FileStatus, List<String>> actualFileStates = checker.getActualFileStates();
        if (actualFileStates.containsKey(FileStatus.STAGED)
                || actualFileStates.containsKey(FileStatus.MODIFIED)
                || actualFileStates.containsKey(FileStatus.DELETED)) {
            return "You have uncommitted changes; please commit them";
        }
        final List<Commit> commitsOfBranch = RepositoryManager.getCommitsUptoRevision(branch, CheckoutKind.BRANCH);
        final List<String> revisionsOfBranch = commitsOfBranch.stream().map(Commit::getHash).collect(Collectors.toList());
        final Commit commitOfHead = Commit.ofHead();
        if (commitOfHead == null) {
            throw new IOException("Internal error: HEAD not found");
        }
        if (revisionsOfBranch.contains(commitOfHead.getHash())) {
            return fastForwardMerge(branch, branchReference);
        } else {
            final Commit mergingBranchCommit = Commit.ofRevision(branchReference);
            Commit commonAncestor = null;
            final List<Commit> commitsOfCurrentBranch = RepositoryManager.getCommitsUptoRevision(currentBranch, CheckoutKind.BRANCH);
            for (Commit nextCommitOfCurrentBranch : commitsOfCurrentBranch) {
                for (Commit nextCommitOfMergingBranch : commitsOfBranch) {
                    if (nextCommitOfCurrentBranch.getHash().equals(nextCommitOfMergingBranch.getHash())) {
                        commonAncestor = nextCommitOfCurrentBranch;
                        break;
                    }
                }
                if (commonAncestor != null) {
                    break;
                }
            }
            if (commonAncestor == null) {
                return "Internal error: every branch must have at least 1 commit in common";
            } else {
                return merge(commitOfHead, mergingBranchCommit, commonAncestor, branch);
            }
        }
    }

    // Commit of current branch is an ancestor of commit of branch with given name
    @NotNull
    private String fastForwardMerge(@NotNull String mergedBranch, @NotNull String mergedBranchReference) throws IOException {
        String originalBranch = currentBranch;
        new Checkout().execute(Collections.singletonList(mergedBranch));
        currentBranch = originalBranch;
        refList.update(currentBranch, mergedBranchReference);
        refList.update(HEAD_REF_NAME, currentBranch);
        refList.write();
        return "Fast forward merge done";
    }

    @NotNull
    private String merge(
            @NotNull Commit currentBranchCommit,
            @NotNull Commit mergingBranchCommit,
            @NotNull Commit commonAncestor,
            @NotNull String mergingBranch
    ) throws IOException {

        final Set<Blob> filesCommittedInCurrentBranch = getFilesCommittedBetween(commonAncestor, currentBranchCommit);
        final Set<Blob> filesCommittedInMergingBranch = getFilesCommittedBetween(commonAncestor, mergingBranchCommit);
        List<Path> filesWithConflicts = new ArrayList<>();
        List<Blob> filesChangedOnlyInMergingBranch = new ArrayList<>();

        for (Blob fileCommittedInCurrentBranch : filesCommittedInCurrentBranch) {

            for (Blob fileCommittedInMergingBranch : filesCommittedInMergingBranch) {

                if (fileCommittedInCurrentBranch.getObjectQualifiedPath().equals(fileCommittedInMergingBranch.getObjectQualifiedPath())) {
                    if (!fileCommittedInCurrentBranch.getEncodedContents().equals(fileCommittedInMergingBranch.getEncodedContents())) {
                        filesWithConflicts.add(fileCommittedInCurrentBranch.getObjectQualifiedPath());
                        writeFileWithConflicts(
                                fileCommittedInCurrentBranch.getObjectQualifiedPath(),
                                fileCommittedInCurrentBranch.decodeContents(),
                                fileCommittedInMergingBranch.decodeContents(),
                                mergingBranch
                        );
                    }
                }

            }

        }
        for (Blob fileCommittedInMergingBranch : filesCommittedInMergingBranch) {
            if (!filesCommittedInCurrentBranch.contains(fileCommittedInMergingBranch)) {
                filesChangedOnlyInMergingBranch.add(fileCommittedInMergingBranch);
            }
        }
        // NPE is impossible here, because existence of given branch was checked
        // in the beginning of `merge` method.
        refList.update(currentBranch, refList.getRevisionForRef(mergingBranch));
        refList.update(HEAD_REF_NAME, currentBranch);
        refList.write();

        List<String> filesToAdd = new ArrayList<>();
        if (!filesChangedOnlyInMergingBranch.isEmpty()) {
            for (Blob fileInMergingBranch : filesChangedOnlyInMergingBranch) {
                writeFileWithoutConflicts(fileInMergingBranch.getObjectQualifiedPath(), fileInMergingBranch.decodeContents());
                filesToAdd.add(fileInMergingBranch.getObjectQualifiedPath().toString());
            }
        }

        Stage stage = Stage.getStage();
        filesToAdd.forEach(stage::addNewFile);

        if (!filesWithConflicts.isEmpty()) {
            StringJoiner answerBuilder = new StringJoiner("\n");
            answerBuilder.add("There are conflicts in the following files:");
            filesWithConflicts.forEach(p -> answerBuilder.add('\t' + p.toString()));
            answerBuilder.add("Following files have no conflicts, they added to staging index:");
            filesToAdd.forEach(f -> answerBuilder.add('\t' + f));
            return answerBuilder.toString();
        } else {
            try {
                stage.commitStage("Merge branch " + mergingBranch);
                StatusChecker checker = new StatusChecker();
                checker.getActualFileStates();
                stage.resetStage();
            } catch (NothingToCommitException ignored) {}
            return "Merge done";
        }
    }

    private Set<Blob> getFilesCommittedBetween(@NotNull Commit ancestor, @NotNull Commit commit) throws IOException {
        Set<Blob> committedFiles = new TreeSet<>(Comparator.comparing(Blob::getObjectQualifiedPath));
        while (!commit.getHash().equals(ancestor.getHash())) {
            committedFiles.addAll(CommitFilesTree.getAllCommittedFiles(GIT_TREES_PATH.resolve(commit.getHash())));
            commit = Commit.ofRevision(commit.getParentHash(0));
        }
        return committedFiles;
    }

    private void writeFileWithoutConflicts(
            @NotNull Path pathToFile,
            @NotNull String fileContents
    ) throws IOException {
        Files.write(pathToFile, Collections.singletonList(fileContents));
    }

    private void writeFileWithConflicts(
            @NotNull Path pathToFile,
            @NotNull String file1Contents,
            @NotNull String file2Contents,
            @NotNull String branchName
    ) throws IOException {
        String result = "<<<<<<< HEAD\n" + file1Contents + "\n=======\n" + file2Contents + "\n>>>>>>> " + branchName;
        Files.write(pathToFile, Collections.singletonList(result));
    }
}
