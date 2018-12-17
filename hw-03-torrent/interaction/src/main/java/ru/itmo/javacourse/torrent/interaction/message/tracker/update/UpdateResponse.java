package ru.itmo.javacourse.torrent.interaction.message.tracker.update;

import org.jetbrains.annotations.NotNull;
import ru.itmo.javacourse.torrent.interaction.message.tracker.TrackerResponse;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class UpdateResponse implements TrackerResponse {

    private final boolean updateStatus;

    public UpdateResponse(boolean updateStatus) {
        this.updateStatus = updateStatus;
    }

    @Override
    public void write(DataOutputStream output) throws IOException {
        output.writeBoolean(updateStatus);
    }

    public static UpdateResponse read(@NotNull final DataInputStream input) throws IOException {
        final boolean updateStatus = input.readBoolean();
        return new UpdateResponse(updateStatus);
    }

    public boolean getUpdateStatus() {
        return updateStatus;
    }
}
