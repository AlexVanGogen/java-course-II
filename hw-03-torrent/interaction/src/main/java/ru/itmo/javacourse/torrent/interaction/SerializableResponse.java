package ru.itmo.javacourse.torrent.interaction;

import java.io.DataOutputStream;
import java.io.IOException;

public interface SerializableResponse {
    void write(DataOutputStream output) throws IOException;
}
