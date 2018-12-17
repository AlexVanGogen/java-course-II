package ru.itmo.javacourse.torrent;

import org.jetbrains.annotations.NotNull;
import ru.itmo.javacourse.torrent.interaction.DistributorDescription;
import ru.itmo.javacourse.torrent.interaction.IpAddress;
import ru.itmo.javacourse.torrent.interaction.filesystem.tracker.DistributedFilesMetadata;
import ru.itmo.javacourse.torrent.interaction.filesystem.tracker.JsonTrackerMetadataReader;
import ru.itmo.javacourse.torrent.interaction.filesystem.tracker.TrackerMetadataReader;
import ru.itmo.javacourse.torrent.interaction.message.tracker.TrackerRequest;
import ru.itmo.javacourse.torrent.interaction.message.tracker.list.ListRequest;
import ru.itmo.javacourse.torrent.interaction.message.tracker.list.ListResponse;
import ru.itmo.javacourse.torrent.interaction.message.tracker.sources.SourcesRequest;
import ru.itmo.javacourse.torrent.interaction.message.tracker.sources.SourcesResponse;
import ru.itmo.javacourse.torrent.interaction.message.tracker.update.UpdateRequest;
import ru.itmo.javacourse.torrent.interaction.message.tracker.update.UpdateResponse;
import ru.itmo.javacourse.torrent.interaction.message.tracker.upload.UploadRequest;
import ru.itmo.javacourse.torrent.interaction.message.tracker.upload.UploadResponse;
import ru.itmo.javacourse.torrent.interaction.protocol.RequestManager;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.stream.Collectors;

import static ru.itmo.javacourse.torrent.interaction.Configuration.*;

public class Tracker {

    @NotNull private final DistributedFilesMetadata distributedFilesMetadata;
    @NotNull private final ServerSocket server;

    public Tracker() throws IOException {
        final TrackerMetadataReader metadataReader = new JsonTrackerMetadataReader();
        final Path pathToMetadataDirectory = Paths.get(TRACKER_FILES_META_PATH_NAME);
        Files.createDirectories(pathToMetadataDirectory);
        final Path pathToMetadataFile = pathToMetadataDirectory.resolve(DISTRIBUTED_FILES_METADATA_FILENAME);
        if (Files.notExists(pathToMetadataFile)) {
            Files.createFile(pathToMetadataFile);
        }
        distributedFilesMetadata = metadataReader.readDistributedFilesMetadata(new FileReader(pathToMetadataFile.toString()));
        server = new ServerSocket(TRACKER_PORT);
    }

    public void launch() {
        new Thread(new TrackerWorker()).start();
    }

    private class TrackerWorker implements Runnable {
        @Override
        public void run() {
            System.out.println("Tracker started at port " + TRACKER_PORT);
            while (true) {
                try (
                        Socket client = server.accept();
                        DataInputStream input = new DataInputStream(client.getInputStream());
                        DataOutputStream output = new DataOutputStream(client.getOutputStream())
                ) {
                    TrackerRequest request = RequestManager.readTrackerRequest(input);
                    if (request instanceof UploadRequest) {
                        final UploadResponse response = executeUpload((UploadRequest) request);
                        response.write(output);
                    } else if (request instanceof ListRequest) {
                        final ListResponse response = executeList((ListRequest) request);
                        response.write(output);
                    } else if (request instanceof SourcesRequest) {
                        final SourcesResponse response = executeSources((SourcesRequest) request);
                        response.write(output);
                    } else {
                        final UpdateResponse response = executeUpdate((UpdateRequest) request, client);
                        response.write(output);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @NotNull
    public UpdateResponse executeUpdate(@NotNull UpdateRequest request, @NotNull Socket client) {
        final IpAddress clientIp = new IpAddress(client.getLocalAddress().getAddress());
        final int clientPort = request.getClientPort();
        final int numberOfUploadedFilesToUpdate = (int) request.getDistributedFilesIdentifiers().stream().filter(id -> distributedFilesMetadata.getFilesIdsAndDescriptions().containsKey(id)).count();
        if (numberOfUploadedFilesToUpdate != request.getDistributedFilesCount()) {
            return new UpdateResponse(false);
        }
        final DistributorDescription description = new DistributorDescription(clientIp, clientPort);
        for (int fileId: request.getDistributedFilesIdentifiers()) {
            distributedFilesMetadata.addDistributor(fileId, description);
        }
        return new UpdateResponse(true);
    }

    @NotNull
    public UploadResponse executeUpload(@NotNull UploadRequest request) throws IOException {
        final int newFileId = distributedFilesMetadata.addFile(request.getFileName(), request.getFileSize());
//        System.out.println("Tracker, upload " + distributedFilesMetadata.getDistributedFilesCount());

//        Path path = Paths.get("some.json");
//        if (Files.notExists(path)) {
//            Files.createFile(path);
//        }
//        new JsonTrackerMetadataWriter().writeDistributedFilesMetadata(distributedFilesMetadata, new FileWriter(path.toFile()));
        return new UploadResponse(newFileId);
    }

    @NotNull
    public ListResponse executeList(@NotNull ListRequest request) {
        return new ListResponse(
                distributedFilesMetadata.getDistributedFilesCount(),
                distributedFilesMetadata
                        .getFilesIdsAndDescriptions()
                        .values()
                        .stream()
                        .map(DistributedFilesMetadata.FileMeta::toFileDescription)
                        .collect(Collectors.toList())
        );
    }

    @NotNull
    public SourcesResponse executeSources(@NotNull SourcesRequest request) {
        final Collection<DistributorDescription> distributors = distributedFilesMetadata.getDistributorsOfFile(request.getFileId());
        return new SourcesResponse(
                distributors.size(),
                distributors
        );
    }
}
