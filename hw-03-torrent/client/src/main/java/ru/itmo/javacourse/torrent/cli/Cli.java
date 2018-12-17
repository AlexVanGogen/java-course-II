package ru.itmo.javacourse.torrent.cli;

import org.jetbrains.annotations.NotNull;
import ru.itmo.javacourse.torrent.Client;
import ru.itmo.javacourse.torrent.command.client.GetCommand;
import ru.itmo.javacourse.torrent.command.tracker.ListCommand;
import ru.itmo.javacourse.torrent.command.tracker.SourcesCommand;
import ru.itmo.javacourse.torrent.command.tracker.UpdateCommand;
import ru.itmo.javacourse.torrent.command.tracker.UploadCommand;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class Cli {

    private Client torrentClient;
    @NotNull private final String helpMessage;

    public Cli() {
        this.helpMessage = "Usage:\n" +
                           "\t- list -- show all tracked files\n" +
                           "\t- upload <filename> -- send filename metadata (name, size) to tracker\n" +
                           "\t- sources <fileid> -- get information about all distributors of file with given id\n" +
                           "\t- update <fileid>* -- send information about files that client is ready to share\n" +
                           "\t- get <fileid> -- download file with given id (or only missing fragments)";
    }

    public void attachClient(@NotNull Client client) {
        torrentClient = client;
    }

    @NotNull
    public List<File> addFilesToUploadPrompt(@NotNull Scanner scanner) {
        System.out.println("Add file paths to upload (empty string means end of input)");

        List<File> filesToUpload = new ArrayList<>();

        while (true) {
            String nextPathAsString = scanner.nextLine();
            if (nextPathAsString.isEmpty()) {
                break;
            }
            filesToUpload.add(new File(nextPathAsString));
        }
        return filesToUpload;
    }

    public void executeRequest(@NotNull String line) throws IOException {
        String[] commandAndArgs = line.split("\\s+");
        if (commandAndArgs.length == 0) {
            throw new ClientCliException();
        }

        switch (commandAndArgs[0]) {
            case "list":
                executeList(commandAndArgs);
                break;
            case "upload":
                executeUpload(commandAndArgs);
                break;
            case "sources":
                executeSources(commandAndArgs);
                break;
            case "update":
                executeUpdate(commandAndArgs);
                break;
            case "get":
                executeGet(commandAndArgs);
                break;
            default:
                throw new ClientCliException();
        }
    }

    public void printHelp() {
        System.out.println(helpMessage);
    }

    private void executeList(String[] commandAndArgs) throws IOException {
        if (commandAndArgs.length > 1) {
            throw new ClientCliException();
        }
        new ListCommand(torrentClient).execute(Arrays.asList(commandAndArgs), System.out);
    }

    private void executeUpload(String[] commandAndArgs) throws IOException {
        if (commandAndArgs.length != 3) {
            throw new ClientCliException();
        }
        new UploadCommand(torrentClient).execute(Arrays.asList(commandAndArgs), System.out);
    }

    private void executeSources(String[] commandAndArgs) throws IOException {
        if (commandAndArgs.length != 2) {
            throw new ClientCliException();
        }
        new SourcesCommand(torrentClient).execute(Arrays.asList(commandAndArgs), System.out);
    }

    private void executeUpdate(String[] commandAndArgs) throws IOException {
        if (commandAndArgs.length < 2) {
            throw new ClientCliException();
        }
        new UpdateCommand(torrentClient).execute(Arrays.asList(commandAndArgs), System.out);
    }

    private void executeGet(String[] commandAndArgs) throws IOException {
        if (commandAndArgs.length != 2) {
            throw new ClientCliException();
        }
        new GetCommand(torrentClient).execute(Arrays.asList(commandAndArgs), System.out);
    }
}