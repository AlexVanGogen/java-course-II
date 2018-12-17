package ru.itmo.javacourse.torrent.interaction.message.tracker.sources;

import org.jetbrains.annotations.NotNull;
import ru.itmo.javacourse.torrent.interaction.message.tracker.TrackerRequest;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class SourcesRequest implements TrackerRequest {
    public static final byte ID = 3;

    private final int fileId;

    public SourcesRequest(int fileId) {
        this.fileId = fileId;
    }

    public int getFileId() {
        return fileId;
    }

    @Override
    public void write(@NotNull DataOutputStream output) throws IOException {
        output.writeByte(ID);
        output.writeInt(fileId);
    }

    public static SourcesRequest read(@NotNull final DataInputStream input) throws IOException {
        final int fileId = input.readInt();
        return new SourcesRequest(fileId);
    }
}
