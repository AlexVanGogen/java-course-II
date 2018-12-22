package ru.itmo.javacourse.torrent;

import org.jetbrains.annotations.NotNull;
import ru.itmo.javacourse.torrent.interaction.filesystem.client.DistributedFilesMetadata;
import ru.itmo.javacourse.torrent.interaction.filesystem.client.FragmentedFile;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class DistributedFilesManager {

    @NotNull private final DistributedFilesMetadata metadata;

    public DistributedFilesManager(@NotNull final Collection<File> availableFiles) {
        final Map<Integer, FragmentedFile> filesAndFragments = new ConcurrentHashMap<>();

        metadata = new DistributedFilesMetadata(filesAndFragments);
    }

    @NotNull
    public DistributedFilesMetadata getMetadata() {
        return metadata;
    }

    public void addFile(int fileId, @NotNull FragmentedFile file) {
        metadata.addFile(fileId, file);
    }

    @NotNull
    public FragmentedFile getFile(int fileId) {
        return metadata.getFile(fileId);
    }

    public int getDistributedFilesCount() {
        return metadata.getFilesAndFragments().size();
    }

    @NotNull
    public Set<Integer> getDistributedFilesIds() {
        return metadata.getFilesAndFragments().keySet();
    }
}
