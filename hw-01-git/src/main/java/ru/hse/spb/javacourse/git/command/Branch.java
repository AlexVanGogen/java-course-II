package ru.hse.spb.javacourse.git.command;

import org.jetbrains.annotations.NotNull;
import ru.hse.spb.javacourse.git.entities.Commit;
import ru.hse.spb.javacourse.git.entities.Ref;
import ru.hse.spb.javacourse.git.entities.RefList;
import ru.hse.spb.javacourse.git.entities.RepositoryManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static ru.hse.spb.javacourse.git.entities.RepositoryManager.HEAD_REF_NAME;
import static ru.hse.spb.javacourse.git.entities.RepositoryManager.commit;
import static ru.hse.spb.javacourse.git.entities.RepositoryManager.currentBranch;

public class Branch extends GitCommand {

    @Override
    public String execute(List<String> args) throws IOException, GitCommandException {
        if (args.size() < 1 || args.size() > 2) {
            throw new GitCommandException();
        }
        if (args.size() == 1) {
            return executeNewBranch(args.get(0));
        } else {
            if (!args.get(0).equals("remove")) {
                return "Unknown command: " + args.get(0);
            }
            return executeRemoveBranch(args.get(1));
        }
    }

    @NotNull
    private String executeNewBranch(@NotNull String branchName) throws IOException {
        RefList refList = new RefList();
        if (refList.getRevisionForRef(branchName) != null) {
            return String.format("Branch with name %s already exists", branchName);
        }
        String headRevision = refList.getRevisionForRef(HEAD_REF_NAME);
        if (headRevision == null) {
            return "Internal error: HEAD ref not found!";
        }
        refList.add(new Ref(branchName, headRevision));
        refList.write();
        return String.format("Branch %s created successfully", branchName);
    }

    @NotNull
    private String executeRemoveBranch(@NotNull String branchName) throws IOException {
        RefList refList = new RefList();
        final String branchRevision = refList.getRevisionForRef(branchName);
        if (branchRevision == null) {
            return String.format("Branch with name %s not found", branchName);
        }
        if (currentBranch.equals(branchName)) {
            return "Cannot remove current branch";
        }
        final List<Commit> commitsOfBranch = RepositoryManager.getCommitsUptoRevision(branchRevision);
        final Set<String> allReferencedRevisions = refList.getAllReferencedRevisions();
        final List<String> allRevisionsUsedByOtherBranches = new ArrayList<>();
        for (final String referencedRevision : allReferencedRevisions) {
            if (!referencedRevision.equals(branchRevision)) {
                allRevisionsUsedByOtherBranches.addAll(RepositoryManager.getCommitsUptoRevision(referencedRevision)
                        .stream()
                        .map(Commit::getHash)
                        .collect(Collectors.toList())
                );
            }
        }
        for (final Commit earlierCommit : commitsOfBranch) {
            if (allRevisionsUsedByOtherBranches.contains(earlierCommit.getHash())) {
                break;
            }
            RepositoryManager.deleteRevisionHistory(earlierCommit.getHash());
        }

        refList.remove(branchName);
        refList.write();
        return String.format("Branch %s removed successfully", branchName);
    }
}
