package ru.itmo.javacourse.torrent.interaction.message.tracker.list;

import org.jetbrains.annotations.NotNull;
import ru.itmo.javacourse.torrent.interaction.DistributedFileDescription;
import ru.itmo.javacourse.torrent.interaction.message.tracker.TrackerResponse;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class ListResponse implements TrackerResponse {

    private final int distributedFilesCount;
    @NotNull private final Collection<DistributedFileDescription> distributedFileDescriptions;

    public ListResponse(int distributedFilesCount, @NotNull Collection<DistributedFileDescription> distributedFileDescriptions) {
        this.distributedFilesCount = distributedFilesCount;
        this.distributedFileDescriptions = distributedFileDescriptions;
    }

    @Override
    public void write(DataOutputStream output) throws IOException {
        output.writeInt(distributedFilesCount);
        for (@NotNull final DistributedFileDescription fileDescription: distributedFileDescriptions) {
            output.writeInt(fileDescription.getFileId());
            output.writeUTF(fileDescription.getFileName());
            output.writeLong(fileDescription.getFileSize());
        }
    }

    public static ListResponse read(@NotNull final DataInputStream input) throws IOException {
        final int distributedFilesCount = input.readInt();
        final Collection<DistributedFileDescription> distributedFileDescriptions = new ArrayList<>();
        for (int i = 0; i < distributedFilesCount; i++) {
            final int fileId = input.readInt();
            final String fileName = input.readUTF();
            final long fileSize = input.readLong();
            distributedFileDescriptions.add(new DistributedFileDescription(fileId, fileName, fileSize));
        }
        return new ListResponse(distributedFilesCount, distributedFileDescriptions);
    }

    public int getDistributedFilesCount() {
        return distributedFilesCount;
    }

    @NotNull
    public Collection<DistributedFileDescription> getDistributedFileDescriptions() {
        return distributedFileDescriptions;
    }
}
