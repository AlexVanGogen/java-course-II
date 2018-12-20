package ru.itmo.javacourse.torrent;

import org.jetbrains.annotations.NotNull;
import ru.itmo.javacourse.torrent.interaction.DistributorDescription;
import ru.itmo.javacourse.torrent.interaction.Notifier;
import ru.itmo.javacourse.torrent.interaction.filesystem.client.FragmentedFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

public class FileDownloader {

    @NotNull private final Map<FragmentedFile, List<DistributorDescription>> downloadingFiles;
    @NotNull private final Client client;
    @NotNull private final DistributedFilesManager filesManager;
    @NotNull private final ExecutorService downloadExecutor;

    public FileDownloader(@NotNull Client client, @NotNull DistributedFilesManager filesManager, @NotNull ExecutorService downloadExecutor) {
        this.downloadingFiles = new ConcurrentHashMap<>();
        this.client = client;
        this.filesManager = filesManager;
        this.downloadExecutor = downloadExecutor;
    }

    public void downloadFile(int fileId, @NotNull String fileName, long fileSize) throws IOException {
        final FragmentedFile newFile = new FragmentedFile(fileId, fileName, fileSize);
        filesManager.addFile(fileId, newFile);
        downloadingFiles.put(newFile, new ArrayList<>(client.executeSources(fileId)));
        downloadExecutor.submit(new DownloadFileTask(newFile));
    }

    private class DownloadFileTask implements Runnable {

        @NotNull private final FragmentedFile fileToDownload;

        public DownloadFileTask(@NotNull FragmentedFile fileToDownload) {
            this.fileToDownload = fileToDownload;
        }

        @Override
        public void run() {
            try {
                while (!fileToDownload.allFragmentsDownloaded()) {
                    List<DistributorDescription> fileDistributors = downloadingFiles.get(fileToDownload);
                    if (fileDistributors.isEmpty()) {
                        fileDistributors = new ArrayList<>(client.executeSources(fileToDownload.getFileId()));
                    }
                    if (fileDistributors.isEmpty()) {
                        continue;
                    }

                    final DistributorDescription selectedDistributor = selectDistributor(fileDistributors);
                    // TODO get rid of cast to short
                    final Collection<Integer> fragmentsIdsToDownload = client.executeStat(fileToDownload.getFileId(), selectedDistributor.getAddress().toString(), (short) selectedDistributor.getPort());
                    for (int fragmentId : fragmentsIdsToDownload) {
                        // TODO allow to download fragments in parallel?
                        if (!fileToDownload.hasFragment(fragmentId)) {
                            downloadFragment(selectedDistributor, fragmentId);
                        }
                    }
                }
                final String pathToNewFile = fileToDownload.unionAllFragmentsToNewFile();

                Notifier.createMessage(String.format("File %s downloaded and stored in %s", fileToDownload.getFileName(), pathToNewFile));
            } catch (IOException e) {
                Notifier.createMessage(String.format("File %s has not downloaded due to I/O error", fileToDownload.getFileName()));
            }
        }

        private void downloadFragment(@NotNull DistributorDescription distributor, int fragmentId) throws IOException {
            final byte[] content = client.executeGet(fileToDownload.getFileId(), fragmentId, distributor.getAddress().toString(), distributor.getPort());
            fileToDownload.downloadFragment(fragmentId, content);
        }

        private DistributorDescription selectDistributor(@NotNull List<DistributorDescription> distributors) {
            return distributors.get(0);
        }
    }
}
