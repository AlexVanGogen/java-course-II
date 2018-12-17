package ru.itmo.javacourse.torrent.interaction;

import org.jetbrains.annotations.NotNull;

import java.io.DataOutputStream;
import java.io.IOException;

public interface SerializableRequest {
    void write(@NotNull DataOutputStream output) throws IOException;
}
