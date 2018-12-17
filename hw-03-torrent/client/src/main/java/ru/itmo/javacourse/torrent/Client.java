package ru.itmo.javacourse.torrent;

import org.jetbrains.annotations.NotNull;
import ru.itmo.javacourse.torrent.interaction.DistributedFileDescription;
import ru.itmo.javacourse.torrent.interaction.DistributorDescription;
import ru.itmo.javacourse.torrent.interaction.filesystem.client.FragmentedFile;
import ru.itmo.javacourse.torrent.interaction.message.client.get.GetRequest;
import ru.itmo.javacourse.torrent.interaction.message.client.get.GetResponse;
import ru.itmo.javacourse.torrent.interaction.message.client.stat.StatRequest;
import ru.itmo.javacourse.torrent.interaction.message.client.stat.StatResponse;
import ru.itmo.javacourse.torrent.interaction.message.tracker.list.ListRequest;
import ru.itmo.javacourse.torrent.interaction.message.tracker.list.ListResponse;
import ru.itmo.javacourse.torrent.interaction.message.tracker.sources.SourcesRequest;
import ru.itmo.javacourse.torrent.interaction.message.tracker.sources.SourcesResponse;
import ru.itmo.javacourse.torrent.interaction.message.tracker.update.UpdateRequest;
import ru.itmo.javacourse.torrent.interaction.message.tracker.update.UpdateResponse;
import ru.itmo.javacourse.torrent.interaction.message.tracker.upload.UploadRequest;
import ru.itmo.javacourse.torrent.interaction.message.tracker.upload.UploadResponse;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static ru.itmo.javacourse.torrent.interaction.Configuration.TRACKER_ADDRESS;
import static ru.itmo.javacourse.torrent.interaction.Configuration.TRACKER_PORT;

// TODO implement updater to make update requests every time
public class Client {

    @NotNull private final FileDownloader downloader;
    @NotNull private final DistributedFilesManager manager;
    @NotNull private final ExecutorService downloadExecutor;
    @NotNull private final Seeder seeder;

    // TODO notifications when files are downloaded
    @NotNull private final Collection<Future<?>> downloadTasksResults;
    private final int port;

    public Client(int port, @NotNull Collection<File> filesToUpload) throws IOException {
        manager = new DistributedFilesManager(filesToUpload);
        downloadExecutor = Executors.newFixedThreadPool(4);
        downloader = new FileDownloader(this, manager, downloadExecutor);
        for (File file : filesToUpload) {
            executeUpload(file.getName(), file.length());
        }
        downloadTasksResults = new ArrayList<>();
        this.port = port;
        seeder = new Seeder(port, manager);
        seeder.launch();
    }

    // TODO sooooo many copypaste, wtf i created protocol interfaces
    @NotNull
    public Collection<DistributedFileDescription> executeList() throws IOException {
        try (
                Socket socket = new Socket(TRACKER_ADDRESS, TRACKER_PORT);
                DataInputStream input = new DataInputStream(socket.getInputStream());
                DataOutputStream output = new DataOutputStream(socket.getOutputStream())
        ) {
            new ListRequest().write(output);

            final ListResponse response = ListResponse.read(input);
            return response.getDistributedFileDescriptions();
        }
    }

    public int executeUpload(@NotNull String fileName, long fileSize) throws IOException {
        try (
                Socket socket = new Socket(TRACKER_ADDRESS, TRACKER_PORT);
                DataInputStream input = new DataInputStream(socket.getInputStream());
                DataOutputStream output = new DataOutputStream(socket.getOutputStream())
        ) {
            new UploadRequest(
                    fileName,
                    fileSize
            ).write(output);

            final UploadResponse response = UploadResponse.read(input);

            final Path pathToFile = Paths.get(fileName);
            final FragmentedFile fragmentedFile = new FragmentedFile(pathToFile.toFile(), response.getUploadedFileId());
            manager.getMetadata().getFilesAndFragments().put(response.getUploadedFileId(), fragmentedFile);
            return response.getUploadedFileId();
        }
    }

    @NotNull
    public Collection<DistributorDescription> executeSources(int fileId) throws IOException {
        try (
                Socket socket = new Socket(TRACKER_ADDRESS, TRACKER_PORT);
                DataInputStream input = new DataInputStream(socket.getInputStream());
                DataOutputStream output = new DataOutputStream(socket.getOutputStream())
        ) {
            new SourcesRequest(fileId).write(output);

            final SourcesResponse response = SourcesResponse.read(input);
            return response.getDistributorsDescriptions();
        }
    }

    public boolean executeUpdate(List<Integer> availableFilesIds) throws IOException {
        try (
                Socket socket = new Socket(TRACKER_ADDRESS, TRACKER_PORT);
                DataInputStream input = new DataInputStream(socket.getInputStream());
                DataOutputStream output = new DataOutputStream(socket.getOutputStream())
        ) {
            new UpdateRequest(port, availableFilesIds.size(), availableFilesIds).write(output);

            final UpdateResponse response = UpdateResponse.read(input);
            return response.getUpdateStatus();
        }
    }

    public void executeDownload(int fileIdToDownload) throws IOException {
        final Collection<DistributedFileDescription> distributedFilesDescriptions = executeList();
        for (DistributedFileDescription fileDescription : distributedFilesDescriptions) {
            if (fileDescription.getFileId() == fileIdToDownload) {
                downloadTasksResults.add(downloader.downloadFile(fileDescription.getFileId(), fileDescription.getFileName(), fileDescription.getFileSize()));
                return;
            }
        }
    }

    @NotNull
    public Collection<Integer> executeStat(int fileId, @NotNull String seederAddress, int seederPort) throws IOException {
        try (
                Socket socket = new Socket(seederAddress, seederPort);
                DataInputStream input = new DataInputStream(socket.getInputStream());
                DataOutputStream output = new DataOutputStream(socket.getOutputStream())
        ) {
            new StatRequest(fileId).write(output);

            final StatResponse response = StatResponse.read(input);
            return response.getAvailablePartsIds();
        }
    }

    public byte[] executeGet(int fileId, int fragmentId, @NotNull String seederAddress, int seederPort) throws IOException {
        try (
                Socket socket = new Socket(seederAddress, seederPort);
                DataInputStream input = new DataInputStream(socket.getInputStream());
                DataOutputStream output = new DataOutputStream(socket.getOutputStream())
        ) {
            new GetRequest(fileId, fragmentId).write(output);

            final GetResponse response = GetResponse.read(input);
            return response.getFragmentContent();
        }
    }

}
