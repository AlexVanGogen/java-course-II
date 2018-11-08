package ru.hse.spb.javacourse.git.entities;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;

import static ru.hse.spb.javacourse.git.entities.RepositoryManager.GIT_MERGING_BRANCH_PATH;

public class MergingState {

    public static void write(@NotNull String mergingBranch) throws IOException {
        Files.write(GIT_MERGING_BRANCH_PATH, Collections.singletonList(mergingBranch));
    }

    @Nullable
    public static String getMergingBranchOrNull() throws IOException {
        return Files.readAllLines(GIT_MERGING_BRANCH_PATH).stream().findFirst().orElse(null);
    }

    public static boolean isRepositoryInMergingState() throws IOException {
        return getMergingBranchOrNull() != null;
    }

    public static void clear() throws IOException {
        Files.write(GIT_MERGING_BRANCH_PATH, Collections.emptyList());
    }
}
