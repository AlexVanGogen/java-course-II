package ru.itmo.javacourse.torrent.interaction.filesystem.tracker;

import org.jetbrains.annotations.NotNull;
import ru.itmo.javacourse.torrent.interaction.DistributedFileDescription;
import ru.itmo.javacourse.torrent.interaction.DistributorDescription;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

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

    public void addOrUpdateDistributor(int fileId, @NotNull DistributorDescription description) {
        filesIdsAndDescriptions.get(fileId).addOrUpdateDistributor(description);
    }

    public void removeOutdatedDistributors() {
        filesIdsAndDescriptions.forEach((ignored, fileMeta) -> fileMeta.removeOutdatedDistributors());
    }

    public void write(@NotNull TrackerMetadataWriter writer, @NotNull FileWriter output) throws IOException {
        writer.writeDistributedFilesMetadata(this, output);
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

        public void addOrUpdateDistributor(@NotNull DistributorDescription distributor) {
            distributor.setLastUpdateTime(LocalDateTime.now());
            for (DistributorDescription nextDistributor : distributors) {
                if (Arrays.equals(nextDistributor.getAddress().getBytes(), distributor.getAddress().getBytes()) && nextDistributor.getPort() == distributor.getPort()) {
                    return;
                }
            }
            distributors.add(distributor);
        }

        public void removeOutdatedDistributors() {
            distributors.removeIf(DistributorDescription::isExpired);
        }
    }
}
