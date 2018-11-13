package ru.hse.spb.javacourse.git.command;

import org.jetbrains.annotations.NotNull;
import ru.hse.spb.javacourse.git.entities.*;
import ru.hse.spb.javacourse.git.entities.Commit;
import ru.hse.spb.javacourse.git.filestatus.StatusChecker;
import ru.hse.spb.javacourse.git.filestree.CommitFilesTree;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static ru.hse.spb.javacourse.git.entities.RepositoryManager.*;

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
        if (args.get(0).equals("--continue")) {
            return continueMerge();
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
            saveInfoAboutFilesWithConflicts(filesWithConflicts);
            StringJoiner answerBuilder = new StringJoiner("\n");
            answerBuilder.add("There are conflicts in the following files:");
            filesWithConflicts.forEach(p -> answerBuilder.add('\t' + p.toString()));
            if (!filesToAdd.isEmpty()) {
                answerBuilder.add("Following files have no conflicts, they added to staging index:");
                filesToAdd.forEach(f -> answerBuilder.add('\t' + f));
            }
            MergingState.write(mergingBranch);
            answerBuilder.add("Resolve conflicts manually and use \"./jgit.sh merge --continue\" to proceed");
            return answerBuilder.toString();
        } else {
            try {
                stage.commitStage("Merge branch " + mergingBranch + " with " + currentBranch);
                StatusChecker checker = new StatusChecker();
                checker.getActualFileStates();
                stage.resetStage();
            } catch (NothingToCommitException ignored) {}
            return "Merge done";
        }
    }

    @NotNull
    private String continueMerge() throws IOException {
        final String mergingBranch = MergingState.getMergingBranchOrNull();
        if (mergingBranch == null) {
            throw new RepositoryNotInMergingStateException();
        }
        final List<String> filesWithConflicts = getInfoAboutFilesWithConflicts();
        System.out.println(filesWithConflicts);
        System.out.println(new Add().execute(filesWithConflicts));
        System.out.println(new ru.hse.spb.javacourse.git.command.Commit().execute(Collections.singletonList("Merge branch " + mergingBranch + " with " + currentBranch)));
        final String revisionOfCurrentBranch = refList.getRevisionForRef(currentBranch);
        if (revisionOfCurrentBranch == null) {
            throw new RevisionNotFoundException("Revision for ref " + currentBranch + " not found");
        }
        refList.update(HEAD_REF_NAME, currentBranch);
        final String revisionOfHead = Commit.ofHead().getHash();
        refList.update(currentBranch, revisionOfHead);
        refList.update(mergingBranch, revisionOfHead);
        refList.write();
        MergingState.clear();
        clearInfoAboutFilesWithConflicts();
        return "Merge done";
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

    private void saveInfoAboutFilesWithConflicts(@NotNull List<Path> pathsToFiles) throws IOException {
        Files.write(GIT_FILES_WITH_CONFLICTS_PATH, pathsToFiles.stream().map(Path::toString).collect(Collectors.toList()));
    }

    @NotNull
    private List<String> getInfoAboutFilesWithConflicts() throws IOException {
        return Files.readAllLines(GIT_FILES_WITH_CONFLICTS_PATH);
    }

    private void clearInfoAboutFilesWithConflicts() throws IOException {
        Files.write(GIT_FILES_WITH_CONFLICTS_PATH, Collections.emptyList());
    }
}
