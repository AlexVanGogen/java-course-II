package ru.itmo.javacourse.torrent.interaction.filesystem.tracker;

import org.jetbrains.annotations.NotNull;
import ru.itmo.javacourse.torrent.interaction.DistributedFileDescription;
import ru.itmo.javacourse.torrent.interaction.DistributorDescription;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * Tracker metadata that consists of distributed files IDs and, for each file,
 * its name, size and active distributors.
 */
public class DistributedFilesMetadata {

    @NotNull private final Map<Integer, FileMeta> filesIdsAndDescriptions;

    public DistributedFilesMetadata(@NotNull Map<Integer, FileMeta> filesIdsAndDescriptions) {
        this.filesIdsAndDescriptions = filesIdsAndDescriptions;
    }

    @NotNull
    public Map<Integer, FileMeta> getFilesIdsAndDescriptions() {
        return filesIdsAndDescriptions;
    }

    @NotNull
    public Collection<DistributorDescription> getDistributorsOfFile(int fileId) {
        final FileMeta distributorsMeta = filesIdsAndDescriptions.get(fileId);
        if (distributorsMeta == null) {
            return Collections.emptyList();
        }
        return distributorsMeta.getDistributors();
    }

    public int getDistributedFilesCount() {
        return filesIdsAndDescriptions.size();
    }

    public int addFile(@NotNull final String fileName, long fileSize) {
        synchronized (filesIdsAndDescriptions) {
            final int newFileId = filesIdsAndDescriptions.size();
            filesIdsAndDescriptions.put(
                    newFileId,
                    new FileMeta(
                            newFileId,
                            fileName,
                            fileSize
                    )
            );
            return newFileId;
        }
    }

    public void addDistributor(int fileId, @NotNull DistributorDescription description) {
        filesIdsAndDescriptions.get(fileId).addDistributor(description);
    }

    public static class FileMeta {

        private final int fileId;
        @NotNull private final String fileName;
        private final long fileSize;
        @NotNull private final Collection<DistributorDescription> distributors;

        public FileMeta(int fileId, @NotNull String fileName, long fileSize) {
            this(fileId, fileName, fileSize, new ArrayList<>());
        }

        public FileMeta(int fileId, @NotNull String fileName, long fileSize, @NotNull Collection<DistributorDescription> distributors) {
            this.fileId = fileId;
            this.fileName = fileName;
            this.fileSize = fileSize;
            this.distributors = distributors;
        }

        public int getFileId() {
            return fileId;
        }

        @NotNull
        public String getFileName() {
            return fileName;
        }

        public long getFileSize() {
            return fileSize;
        }

        @NotNull
        public Collection<DistributorDescription> getDistributors() {
            return distributors;
        }

        @NotNull
        public DistributedFileDescription toFileDescription() {
            return new DistributedFileDescription(fileId, fileName, fileSize);
        }

        public void addDistributor(@NotNull DistributorDescription distributor) {
            distributors.add(distributor);
        }
    }
}
