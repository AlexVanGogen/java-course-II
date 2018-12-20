package ru.itmo.javacourse.torrent;

import org.jetbrains.annotations.NotNull;
import ru.itmo.javacourse.torrent.interaction.DistributorDescription;
import ru.itmo.javacourse.torrent.interaction.IpAddress;
import ru.itmo.javacourse.torrent.interaction.Notifier;
import ru.itmo.javacourse.torrent.interaction.Response;
import ru.itmo.javacourse.torrent.interaction.filesystem.tracker.DistributedFilesMetadata;
import ru.itmo.javacourse.torrent.interaction.filesystem.tracker.JsonTrackerMetadataReader;
import ru.itmo.javacourse.torrent.interaction.filesystem.tracker.JsonTrackerMetadataWriter;
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
import ru.itmo.javacourse.torrent.interaction.protocol.RequestProvider;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.stream.Collectors;

import static ru.itmo.javacourse.torrent.interaction.Configuration.*;

public class Tracker implements AutoCloseable {

    @NotNull private final DistributedFilesMetadata distributedFilesMetadata;
    @NotNull private final ServerSocket server;
    @NotNull private final Thread mainWorker;
    @NotNull private final Thread distributorsRemover;

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

        mainWorker = new Thread(new TrackerWorker());
        distributorsRemover = new Thread(new DistributorsRemover());
    }

    public void launch() {
        mainWorker.start();
        distributorsRemover.start();
    }

    private class TrackerWorker implements Runnable {
        @Override
        public void run() {
            Notifier.createTrackerMessage("Tracker started at port " + TRACKER_PORT);
            while (true) {
                try (
                        Socket client = server.accept();
                        DataInputStream input = new DataInputStream(client.getInputStream());
                        DataOutputStream output = new DataOutputStream(client.getOutputStream())
                ) {
                    TrackerRequest request = RequestProvider.getTrackerRequest(input);
                    Response response;
                    if (request instanceof UploadRequest) {
                        response = executeUpload((UploadRequest) request);
                    } else if (request instanceof ListRequest) {
                        response = executeList((ListRequest) request);
                    } else if (request instanceof SourcesRequest) {
                        response = executeSources((SourcesRequest) request);
                    } else {
                        response = executeUpdate((UpdateRequest) request, client);
                    }
                    response.write(output);
                } catch (IOException e) {
                    Notifier.createTrackerMessage("I/O error happened during tracker lifecycle");
                }
            }
        }
    }

    private class DistributorsRemover implements Runnable {

        @Override
        public void run() {
            while (true) {
                distributedFilesMetadata.removeOutdatedDistributors();
                try {
                    Thread.sleep(TRACKER_CHECK_OUTDATED_PERIOD_MILLIS);
                } catch (InterruptedException ignored) { }
            }
        }
    }

    @NotNull
    public UpdateResponse executeUpdate(@NotNull UpdateRequest request, @NotNull Socket client) {
        final IpAddress clientIp = new IpAddress(((InetSocketAddress)client.getRemoteSocketAddress()).getAddress().getAddress());
        final short clientPort = request.getClientPort();
        final int numberOfUploadedFilesToUpdate = (int) request.getDistributedFilesIdentifiers().stream().filter(id -> distributedFilesMetadata.getFilesIdsAndDescriptions().containsKey(id)).count();
        if (numberOfUploadedFilesToUpdate != request.getDistributedFilesCount()) {
            return new UpdateResponse(false);
        }
        final DistributorDescription description = new DistributorDescription(clientIp, clientPort);
        for (int fileId: request.getDistributedFilesIdentifiers()) {
            distributedFilesMetadata.addOrUpdateDistributor(fileId, description);
        }
        return new UpdateResponse(true);
    }

    @NotNull
    public UploadResponse executeUpload(@NotNull UploadRequest request) throws IOException {
        final int newFileId = distributedFilesMetadata.addFile(request.getFileName(), request.getFileSize());
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

    @Override
    public void close() throws Exception {
        mainWorker.join();
        distributorsRemover.join();
        server.close();
        distributedFilesMetadata.write(new JsonTrackerMetadataWriter(), new FileWriter(DISTRIBUTED_FILES_METADATA_FILENAME));
    }
}
