package ru.itmo.javacourse.torrent;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.Socket;

import static ru.itmo.javacourse.torrent.interaction.Configuration.TRACKER_ADDRESS;
import static ru.itmo.javacourse.torrent.interaction.Configuration.TRACKER_PORT;

public class SocketProvider {

    @NotNull
    public static Socket getSocketForTracker() throws IOException {
        return new Socket(TRACKER_ADDRESS, TRACKER_PORT);
    }

    @NotNull
    public static Socket getSocketForSeeder(@NotNull String seederAddress, short seederPort) throws IOException {
        return new Socket(seederAddress, seederPort);
    }
}
