package ru.itmo.javacourse.torrent.interaction.message.tracker.list;

import org.jetbrains.annotations.NotNull;
import ru.itmo.javacourse.torrent.interaction.message.tracker.TrackerRequest;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ListRequest implements TrackerRequest {
    public static final byte ID = 1;

    @Override
    public void write(@NotNull DataOutputStream output) throws IOException {
        output.writeByte(ID);
    }

    public static ListRequest read(@NotNull final DataInputStream input) throws IOException {
        return new ListRequest();
    }
}
