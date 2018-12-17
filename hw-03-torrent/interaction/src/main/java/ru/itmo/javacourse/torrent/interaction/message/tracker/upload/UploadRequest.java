package ru.itmo.javacourse.torrent.interaction.message.tracker.upload;

import org.jetbrains.annotations.NotNull;
import ru.itmo.javacourse.torrent.interaction.message.tracker.TrackerRequest;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class UploadRequest implements TrackerRequest {
    public static final byte ID = 2;

    @NotNull private final String fileName;
    private final long fileSize;

    public UploadRequest(@NotNull String fileName, long fileSize) {
        this.fileName = fileName;
        this.fileSize = fileSize;
    }

    @Override
    public void write(@NotNull DataOutputStream output) throws IOException {
        output.writeByte(ID);
        output.writeUTF(fileName);
        output.writeLong(fileSize);
        output.flush();
    }

    public static UploadRequest read(@NotNull final DataInputStream input) throws IOException {
        final String fileName = input.readUTF();
        final long fileSize = input.readLong();
//        final IpAddress address = IpAddress.get(input);
//        final int port = input.readInt();
        return new UploadRequest(fileName, fileSize);
    }

    @NotNull
    public String getFileName() {
        return fileName;
    }

    public long getFileSize() {
        return fileSize;
    }
}
