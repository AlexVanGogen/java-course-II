package ru.itmo.javacourse.torrent.interaction.protocol;

import org.jetbrains.annotations.NotNull;
import ru.itmo.javacourse.torrent.interaction.Request;
import ru.itmo.javacourse.torrent.interaction.Response;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public interface Protocol extends AutoCloseable {

    void sendRequest(@NotNull Request request) throws IOException;

    @NotNull
    <R extends Response> R receiveResponse(@NotNull Class<R> responseClass) throws IOException;
}
