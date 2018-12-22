package ru.itmo.javacourse.torrent.interaction.filesystem.client;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class DistributedFilesMetadata {

    @NotNull private final Map<Integer, FragmentedFile> filesAndFragments;

    public DistributedFilesMetadata(@NotNull Map<Integer, FragmentedFile> filesAndFragments) {
        this.filesAndFragments = filesAndFragments;
    }

    @NotNull
    public Map<Integer, FragmentedFile> getFilesAndFragments() {
        return filesAndFragments;
    }

    public void addFile(int fileId, FragmentedFile file) {
        filesAndFragments.put(fileId, file);
    }

    @NotNull
    public FragmentedFile getFile(int fileId) {
        return filesAndFragments.get(fileId);
    }
}
