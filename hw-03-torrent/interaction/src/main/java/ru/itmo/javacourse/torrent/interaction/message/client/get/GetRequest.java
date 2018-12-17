package ru.itmo.javacourse.torrent.interaction.message.client.get;

import org.jetbrains.annotations.NotNull;
import ru.itmo.javacourse.torrent.interaction.message.client.ClientRequest;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class GetRequest implements ClientRequest {
    public static final byte ID = 2;

    private final int fileId;
    private final int fileFragmentId;

    public GetRequest(int fileId, int fileFragmentId) {
        this.fileId = fileId;
        this.fileFragmentId = fileFragmentId;
    }

    @Override
    public void write(@NotNull DataOutputStream output) throws IOException {
        output.writeByte(ID);
        output.writeInt(fileId);
        output.writeInt(fileFragmentId);
    }

    public static GetRequest read(@NotNull final DataInputStream input) throws IOException {
        final int fileId = input.readInt();
        final int fragmentId = input.readInt();
        return new GetRequest(fileId, fragmentId);
    }

    public int getFileId() {
        return fileId;
    }

    public int getFileFragmentId() {
        return fileFragmentId;
    }
}
