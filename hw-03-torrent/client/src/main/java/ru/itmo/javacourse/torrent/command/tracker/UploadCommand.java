package ru.itmo.javacourse.torrent.command.tracker;

import org.jetbrains.annotations.NotNull;
import ru.itmo.javacourse.torrent.Client;
import ru.itmo.javacourse.torrent.command.AbstractCommand;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class UploadCommand implements AbstractCommand {

    @NotNull final Client client;

    public UploadCommand(@NotNull Client client) {
        this.client = client;
    }

    @Override
    public void execute(@NotNull List<String> args, @NotNull PrintStream output) throws IOException {
        final String fileName = args.get(1);
        final long fileSize = getSizeOfFileWithName(fileName);
        final int uploadedFileId = client.executeUpload(fileName, fileSize);
        output.println("File " + fileName + " uploaded, id is " + uploadedFileId);
    }

    private long getSizeOfFileWithName(@NotNull String fileName) throws IOException {
        return Files.size(Paths.get(fileName));
    }
}
