package ru.itmo.javacourse.torrent.interaction.message.client.stat;

import org.jetbrains.annotations.NotNull;
import ru.itmo.javacourse.torrent.interaction.message.client.ClientRequest;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class StatRequest implements ClientRequest {
    public static final byte ID = 1;

    private final int fileId;

    public StatRequest(int fileId) {
        this.fileId = fileId;
    }

    @Override
    public void write(@NotNull DataOutputStream output) throws IOException {
        output.writeByte(ID);
        output.writeInt(fileId);
    }

    public static StatRequest read(@NotNull final DataInputStream input) throws IOException {
        final int fileId = input.readInt();
        return new StatRequest(fileId);
    }

    public int getFileId() {
        return fileId;
    }
}
