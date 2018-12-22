package ru.itmo.javacourse.torrent.command.tracker;

import org.jetbrains.annotations.NotNull;
import ru.itmo.javacourse.torrent.Client;
import ru.itmo.javacourse.torrent.command.AbstractCommand;
import ru.itmo.javacourse.torrent.interaction.DistributedFileDescription;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.List;

public class ListCommand implements AbstractCommand {

    @NotNull final Client client;

    public ListCommand(@NotNull Client client) {
        this.client = client;
    }

    @Override
    public void execute(@NotNull List<String> args, @NotNull PrintStream output) throws IOException {
        final Collection<DistributedFileDescription> fileDescriptions = client.executeList();
        output.println("Tracked files:");
        for (DistributedFileDescription fileDescription : fileDescriptions) {
            output.println(fileDescription.getFileId() + " " + fileDescription.getFileName() + " " + fileDescription.getFileSize());
        }
    }
}
