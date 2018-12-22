package ru.itmo.javacourse.torrent.command.tracker;

import org.jetbrains.annotations.NotNull;
import ru.itmo.javacourse.torrent.Client;
import ru.itmo.javacourse.torrent.command.AbstractCommand;
import ru.itmo.javacourse.torrent.interaction.DistributorDescription;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.List;

public class SourcesCommand implements AbstractCommand {

    @NotNull final Client client;

    public SourcesCommand(@NotNull Client client) {
        this.client = client;
    }

    @Override
    public void execute(@NotNull List<String> args, @NotNull PrintStream output) throws IOException {
        final Integer fileId = Integer.valueOf(args.get(1));
        final Collection<DistributorDescription> distributors = client.executeSources(fileId);
        if (distributors.isEmpty()) {
            output.println("No distributors of file with id " + fileId);
            return;
        }
        output.println("Distributors of file with id " + fileId + ":");
        for (DistributorDescription distributor : distributors) {
            output.println(distributor.getAddress().toString() + " " + distributor.getPort());
        }
    }
}
