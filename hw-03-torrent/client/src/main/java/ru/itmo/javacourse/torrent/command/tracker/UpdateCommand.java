package ru.itmo.javacourse.torrent.command.tracker;

import org.jetbrains.annotations.NotNull;
import ru.itmo.javacourse.torrent.Client;
import ru.itmo.javacourse.torrent.command.AbstractCommand;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.stream.Collectors;

public class UpdateCommand implements AbstractCommand {

    @NotNull final Client client;

    public UpdateCommand(@NotNull Client client) {
        this.client = client;
    }

    @Override
    public void execute(@NotNull List<String> args, @NotNull PrintStream output) throws IOException {
        final List<Integer> updatedFilesIds = args.subList(1, args.size()).stream().map(Integer::valueOf).collect(Collectors.toList());
        final boolean status = client.executeUpdate(updatedFilesIds);
        output.println("Update " + (status ? "successful" : "unsuccessful"));
    }
}
