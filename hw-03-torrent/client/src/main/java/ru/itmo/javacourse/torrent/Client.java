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
import ru.itmo.javacourse.torrent.interaction.protocol.ProtocolImpl;

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

    private final short port;

    public Client(short port, @NotNull Collection<File> filesToUpload) throws IOException {
        manager = new DistributedFilesManager(filesToUpload);
        downloadExecutor = Executors.newFixedThreadPool(4);
        downloader = new FileDownloader(this, manager, downloadExecutor);
        for (File file : filesToUpload) {
            executeUpload(file.getName(), file.length());
        }
        this.port = port;
        seeder = new Seeder(port, manager);
        seeder.launch();
    }

    @NotNull
    public Collection<DistributedFileDescription> executeList() throws IOException {
        final ProtocolImpl protocol = new ProtocolImpl(SocketProvider.getSocketForTracker());
        protocol.sendRequest(new ListRequest());

        final ListResponse response = protocol.receiveResponse(ListResponse.class);
        return response.getDistributedFileDescriptions();
    }

    public int executeUpload(@NotNull String fileName, long fileSize) throws IOException {
        final ProtocolImpl protocol = new ProtocolImpl(SocketProvider.getSocketForTracker());
        protocol.sendRequest(new UploadRequest(fileName, fileSize));

        final UploadResponse response = protocol.receiveResponse(UploadResponse.class);
        final Path pathToFile = Paths.get(fileName);
        final FragmentedFile fragmentedFile = new FragmentedFile(pathToFile.toFile(), response.getUploadedFileId());
        manager.getMetadata().getFilesAndFragments().put(response.getUploadedFileId(), fragmentedFile);
        return response.getUploadedFileId();
    }

    @NotNull
    public Collection<DistributorDescription> executeSources(int fileId) throws IOException {
        final ProtocolImpl protocol = new ProtocolImpl(SocketProvider.getSocketForTracker());
        protocol.sendRequest(new SourcesRequest(fileId));

        final SourcesResponse response = protocol.receiveResponse(SourcesResponse.class);
        return response.getDistributorsDescriptions();
    }

    public boolean executeUpdate(List<Integer> availableFilesIds) throws IOException {
        final ProtocolImpl protocol = new ProtocolImpl(SocketProvider.getSocketForTracker());
        protocol.sendRequest(new UpdateRequest(port, availableFilesIds.size(), availableFilesIds));

        final UpdateResponse response = protocol.receiveResponse(UpdateResponse.class);
        return response.getUpdateStatus();
    }

    public void executeDownload(int fileIdToDownload) throws IOException {
        final Collection<DistributedFileDescription> distributedFilesDescriptions = executeList();
        for (DistributedFileDescription fileDescription : distributedFilesDescriptions) {
            if (fileDescription.getFileId() == fileIdToDownload) {
                downloader.downloadFile(fileDescription.getFileId(), fileDescription.getFileName(), fileDescription.getFileSize());
                return;
            }
        }
    }

    @NotNull
    public Collection<Integer> executeStat(int fileId, @NotNull String seederAddress, short seederPort) throws IOException {
        final ProtocolImpl protocol = new ProtocolImpl(SocketProvider.getSocketForSeeder(seederAddress, seederPort));
        protocol.sendRequest(new StatRequest(fileId));

        final StatResponse response = protocol.receiveResponse(StatResponse.class);
        return response.getAvailablePartsIds();
    }

    public byte[] executeGet(int fileId, int fragmentId, @NotNull String seederAddress, short seederPort) throws IOException {
        final ProtocolImpl protocol = new ProtocolImpl(SocketProvider.getSocketForSeeder(seederAddress, seederPort));
        protocol.sendRequest(new GetRequest(fileId, fragmentId));

        final GetResponse response = protocol.receiveResponse(GetResponse.class);
        return response.getFragmentContent();
    }
}
