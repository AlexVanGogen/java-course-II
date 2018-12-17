package ru.itmo.javacourse.torrent.interaction.message.tracker.update;

import org.jetbrains.annotations.NotNull;
import ru.itmo.javacourse.torrent.interaction.message.tracker.TrackerRequest;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class UpdateRequest implements TrackerRequest {
    public static final byte ID = 4;
    public static final int REFRESH_TIME_SECS = 300;

    private final int clientPort;
    private final int distributedFilesCount;
    @NotNull private final Collection<Integer> distributedFilesIdentifiers;

    public UpdateRequest(int clientPort, int distributedFilesCount, @NotNull Collection<Integer> distributedFilesIdentifiers) {
        this.clientPort = clientPort;
        this.distributedFilesCount = distributedFilesCount;
        this.distributedFilesIdentifiers = distributedFilesIdentifiers;
    }

    @Override
    public void write(@NotNull DataOutputStream output) throws IOException {
        output.writeByte(ID);
        output.writeShort(clientPort);
        output.writeInt(distributedFilesCount);
        for (final int fileId : distributedFilesIdentifiers) {
            output.writeInt(fileId);
        }
    }

    public static UpdateRequest read(@NotNull final DataInputStream input) throws IOException {
        final short clientPort = input.readShort();
        final int distributedFilesCount = input.readInt();
        final Collection<Integer> distributedFilesIdentifiers = new ArrayList<>();
        for (int i = 0; i < distributedFilesCount; i++) {
            final int nextFileId = input.readInt();
            distributedFilesIdentifiers.add(nextFileId);
        }
        return new UpdateRequest(clientPort, distributedFilesCount, distributedFilesIdentifiers);
    }

    public int getClientPort() {
        return clientPort;
    }

    public int getDistributedFilesCount() {
        return distributedFilesCount;
    }

    @NotNull
    public Collection<Integer> getDistributedFilesIdentifiers() {
        return distributedFilesIdentifiers;
    }
}
