package ru.itmo.javacourse.torrent.command.client;

import org.jetbrains.annotations.NotNull;
import ru.itmo.javacourse.torrent.Client;
import ru.itmo.javacourse.torrent.command.AbstractCommand;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

public class GetCommand implements AbstractCommand {

    @NotNull final Client client;

    public GetCommand(@NotNull Client client) {
        this.client = client;
    }

    @Override
    public void execute(@NotNull List<String> args, @NotNull PrintStream output) throws IOException {
        final int fileId = Integer.valueOf(args.get(1));
        client.executeDownload(fileId);
    }
}
