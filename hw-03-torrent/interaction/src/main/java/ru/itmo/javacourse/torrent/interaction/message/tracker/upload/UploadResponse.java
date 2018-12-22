package ru.itmo.javacourse.torrent.interaction.message.tracker.upload;

import org.jetbrains.annotations.NotNull;
import ru.itmo.javacourse.torrent.interaction.message.tracker.TrackerResponse;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class UploadResponse implements TrackerResponse {
    private final int uploadedFileId;

    public UploadResponse(int uploadedFileId) {
        this.uploadedFileId = uploadedFileId;
    }

    @Override
    public void write(DataOutputStream output) throws IOException {
        output.writeInt(uploadedFileId);
        output.flush();
    }

    public static UploadResponse read(@NotNull final DataInputStream input) throws IOException {
        final int uploadedFileId = input.readInt();
        return new UploadResponse(uploadedFileId);
    }

    public int getUploadedFileId() {
        return uploadedFileId;
    }
}
