package ru.itmo.javacourse.torrent;

import org.jetbrains.annotations.NotNull;
import ru.itmo.javacourse.torrent.interaction.filesystem.client.FragmentedFile;
import ru.itmo.javacourse.torrent.interaction.message.client.get.GetRequest;
import ru.itmo.javacourse.torrent.interaction.message.client.get.GetResponse;
import ru.itmo.javacourse.torrent.interaction.message.client.stat.StatRequest;
import ru.itmo.javacourse.torrent.interaction.message.client.stat.StatResponse;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Seeder {

    @NotNull private final DistributedFilesManager manager;
    @NotNull private final ServerSocket seeder;

    public Seeder(int port, @NotNull DistributedFilesManager manager) throws IOException {
        this.seeder = new ServerSocket(port);
        this.manager = manager;
    }

    public void launch() {
        new Thread(new SeederWorker()).start();
    }

    private class SeederWorker implements Runnable {
        @Override
        public void run() {
            while (true) {
                try (
                        Socket client = seeder.accept();
                        DataInputStream input = new DataInputStream(client.getInputStream());
                        DataOutputStream output = new DataOutputStream(client.getOutputStream())
                ) {
                    // TODO use RequestManager (like Tracker does)
                    switch (input.readByte()) {
                        case 1:
                            executeStat(StatRequest.read(input)).write(output);
                        case 2:
                            executeGet(GetRequest.read(input)).write(output);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @NotNull
    public StatResponse executeStat(@NotNull StatRequest request) {
        final FragmentedFile requestedFile = manager.getFile(request.getFileId());
        return new StatResponse(
                requestedFile.getNumberOfAvailableFragments(),
                new ArrayList<>(requestedFile.getAvailableFragments().keySet())
        );
    }

    @NotNull
    public GetResponse executeGet(@NotNull GetRequest request) throws IOException {
        final FragmentedFile requestedFile = manager.getFile(request.getFileId());
        final byte[] content = requestedFile.getFragmentData(request.getFileFragmentId());
        return new GetResponse(content);
    }
}
