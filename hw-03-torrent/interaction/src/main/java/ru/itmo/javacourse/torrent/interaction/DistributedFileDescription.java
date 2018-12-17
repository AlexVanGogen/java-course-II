package ru.itmo.javacourse.torrent.interaction;

import org.jetbrains.annotations.NotNull;

public class DistributedFileDescription {
    private final int fileId;
    @NotNull private final String fileName;
    private final long fileSize;

    public DistributedFileDescription(int fileId, @NotNull String fileName, long fileSize) {
        this.fileId = fileId;
        this.fileName = fileName;
        this.fileSize = fileSize;
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
}
