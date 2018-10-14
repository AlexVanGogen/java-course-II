package ru.hse.spb.javacourse.git.command;

import org.jetbrains.annotations.NotNull;
import ru.hse.spb.javacourse.git.entities.CheckoutKind;
import ru.hse.spb.javacourse.git.entities.Commit;
import ru.hse.spb.javacourse.git.entities.RefList;
import ru.hse.spb.javacourse.git.entities.RepositoryManager;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
        final List<Commit> commitsOfBranch = RepositoryManager.getCommitsUptoRevision(branch, CheckoutKind.BRANCH);
        final List<String> revisionsOfBranch = commitsOfBranch.stream().map(Commit::getHash).collect(Collectors.toList());
        final Commit commitOfHead = Commit.ofHead();
        if (commitOfHead != null && revisionsOfBranch.contains(commitOfHead.getHash())) {
            return fastForwardMerge(branch, branchReference);
        }
        // TODO
        return "";
    }

    // Commit of current branch is an ancestor of commit of branch with given name
    private String fastForwardMerge(@NotNull String mergedBranch, @NotNull String mergedBranchReference) throws IOException {
        String originalBranch = currentBranch;
        new Checkout().execute(Collections.singletonList(mergedBranch));
        currentBranch = originalBranch;
        refList.update(currentBranch, mergedBranchReference);
        refList.update(HEAD_REF_NAME, currentBranch);
        refList.write();
        return "Fast forward merge done";
    }
}
