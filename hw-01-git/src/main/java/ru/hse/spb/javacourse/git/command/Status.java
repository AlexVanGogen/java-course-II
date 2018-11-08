package ru.hse.spb.javacourse.git.command;

import ru.hse.spb.javacourse.git.entities.MergingState;
import ru.hse.spb.javacourse.git.filestatus.FileStatus;
import ru.hse.spb.javacourse.git.filestatus.StatusChecker;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static ru.hse.spb.javacourse.git.entities.RepositoryManager.currentBranch;

public class Status extends GitCommand {

    @Override
    public String execute(List<String> args) throws IOException, GitCommandException {
        if (!args.isEmpty()) {
            throw new GitCommandException();
        }
        StatusChecker statusChecker = new StatusChecker();
        Map<FileStatus, List<String>> fileStates = statusChecker.getActualFileStates();

        StringBuilder statusMessageBuilder = new StringBuilder();

        statusMessageBuilder.append("On branch ").append(currentBranch).append("\n");
        final String mergingBranch = MergingState.getMergingBranchOrNull();
        if (mergingBranch != null) {
            statusMessageBuilder.append("Merging branch ").append(mergingBranch).append("\n");
        }

        if (fileStates.containsKey(FileStatus.STAGED)) {
            Collections.sort(fileStates.get(FileStatus.STAGED));
            statusMessageBuilder.append("Staged files:\n\t").append(String.join("\n\t", fileStates.get(FileStatus.STAGED)));
        }
        if (fileStates.containsKey(FileStatus.MODIFIED)) {
            Collections.sort(fileStates.get(FileStatus.MODIFIED));
            statusMessageBuilder.append("\nModified files:\n\t").append(String.join("\n\t", fileStates.get(FileStatus.MODIFIED)));
        }
        if (fileStates.containsKey(FileStatus.DELETED)) {
            Collections.sort(fileStates.get(FileStatus.DELETED));
            statusMessageBuilder.append("\nDeleted files:\n\t").append(String.join("\n\t", fileStates.get(FileStatus.DELETED)));
        }
        return statusMessageBuilder.toString();
    }
}
