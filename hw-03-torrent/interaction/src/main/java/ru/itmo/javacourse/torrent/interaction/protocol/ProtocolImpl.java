package ru.itmo.javacourse.torrent.interaction.protocol;

import org.jetbrains.annotations.NotNull;
import ru.itmo.javacourse.torrent.interaction.Notifier;
import ru.itmo.javacourse.torrent.interaction.Request;
import ru.itmo.javacourse.torrent.interaction.Response;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Socket;

public class ProtocolImpl implements Protocol {

    @NotNull private final Socket clientSocket;
    @NotNull private final DataInputStream input;
    @NotNull private final DataOutputStream output;

    public ProtocolImpl(@NotNull Socket clientSocket) throws IOException {
        this.clientSocket = clientSocket;
        input = new DataInputStream(clientSocket.getInputStream());
        output = new DataOutputStream(clientSocket.getOutputStream());
    }

    @Override
    public void sendRequest(@NotNull Request request) throws IOException {
        request.write(output);
        output.flush();
    }

    // I don't actually know is using reflection here is good idea,
    // but it completely saves protocol logic from code duplication.
    @NotNull
    @Override
    public <R extends Response> R receiveResponse(@NotNull Class<R> responseClass) throws IOException {
        final Method readingMethod;
        try {
            readingMethod = responseClass.getMethod("read", DataInputStream.class);
            return responseClass.cast(readingMethod.invoke(null, input));
        } catch (NoSuchMethodException e) {
            Notifier.createMessage("Cannot find method read in class " + responseClass.getCanonicalName());
            final IOException exception = new IOException();
            exception.addSuppressed(e);
            throw exception;
        } catch (IllegalAccessException | InvocationTargetException e) {
            Notifier.createMessage("Error when invoking method read in class " + responseClass.getCanonicalName());
            final IOException exception = new IOException();
            exception.addSuppressed(e);
            throw exception;
        }
    }

    @Override
    public void close() throws Exception {
        clientSocket.close();
        input.close();
        output.close();
    }
}
