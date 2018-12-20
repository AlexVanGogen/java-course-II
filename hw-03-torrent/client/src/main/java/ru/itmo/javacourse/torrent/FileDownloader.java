package ru.itmo.javacourse.torrent;

import org.jetbrains.annotations.NotNull;
import ru.itmo.javacourse.torrent.interaction.DistributorDescription;
import ru.itmo.javacourse.torrent.interaction.Notifier;
import ru.itmo.javacourse.torrent.interaction.filesystem.client.FragmentedFile;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

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
        @NotNull private final List<Future<?>> downloadedFragmentsFutures = new ArrayList<>();
        @NotNull private final CompletableFuture<?> downloadedFileFuture = new CompletableFuture<>();

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
                    final Collection<Integer> fragmentsIdsToDownload = client.executeStat(fileToDownload.getFileId(), selectedDistributor.getAddress().toString(), (short) selectedDistributor.getPort());
                    downloadExecutor.invokeAll(
                            fragmentsIdsToDownload.stream()
                                    .filter(id -> !fileToDownload.hasFragment(id))
                                    .map(id -> new DownloadFragmentTask(fileToDownload, selectedDistributor, id))
                                    .collect(Collectors.toList())
                    );
                    if (!fileToDownload.getAvailableFragments().isEmpty()) {
                        client.executeUpdate(Collections.singletonList(fileToDownload.getFileId()));
                    }
                }

                final String pathToNewFile = fileToDownload.unionAllFragmentsToNewFile();

                Notifier.createClientMessage(String.format("File %s downloaded and stored in %s", fileToDownload.getFileName(), pathToNewFile));
            } catch (IOException e) {
                Notifier.createClientMessage(String.format("File %s has not downloaded due to I/O error", fileToDownload.getFileName()));
            } catch (InterruptedException e) {
                Notifier.createClientMessage(String.format("File %s has not downloaded due to ExecutorService error", fileToDownload.getFileName()));
            }
        }

        private DistributorDescription selectDistributor(@NotNull List<DistributorDescription> distributors) {
            return distributors.get(0);
        }
    }

    private class DownloadFragmentTask implements Callable<Object> {

        @NotNull private final FragmentedFile fileToDownload;
        @NotNull private final DistributorDescription selectedDistributor;
        private final int fragmentId;

        public DownloadFragmentTask(@NotNull FragmentedFile fileToDownload, @NotNull DistributorDescription selectedDistributor, int fragmentId) {
            this.fileToDownload = fileToDownload;
            this.selectedDistributor = selectedDistributor;
            this.fragmentId = fragmentId;
        }

        @Override
        public Object call() {
            try {
                downloadFragment(selectedDistributor, fragmentId);
            } catch (IOException e) {
                Notifier.createClientMessage(String.format("Fragment %d of file %s has not downloaded due to I/O error", fragmentId, fileToDownload.getFileName()));
            }
            return null;
        }

        private void downloadFragment(@NotNull DistributorDescription distributor, int fragmentId) throws IOException {
            final byte[] content = client.executeGet(fileToDownload.getFileId(), fragmentId, distributor.getAddress().toString(), distributor.getPort());
            fileToDownload.downloadFragment(fragmentId, content);
        }
    }
}
