package ru.itmo.javacourse.torrent.interaction.filesystem.client;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

import static ru.itmo.javacourse.torrent.interaction.Configuration.DEFAULT_FRAGMENT_SIZE_BYTES;
import static ru.itmo.javacourse.torrent.interaction.Configuration.FRAGMENTS_PATH_NAME;

public class FragmentedFile {

    private final int fileId;
    @NotNull private final String fileName;
    private final long fileSize;
    @NotNull private final Map<Integer, FileFragment> availableFragments = new ConcurrentHashMap<>();
    @NotNull private final Path pathToFileFragments;
    private final int numberOfFragments;
    @NotNull private final Set<Integer> fragmentsIdsToDownload = Collections.synchronizedSet(new HashSet<>());
    @Nullable private RandomAccessFile fullFile;

    public FragmentedFile(@NotNull File originalFile, int fileId) throws IOException {
        this.fileId = fileId;
        fileName = originalFile.getName();
        this.fileSize = originalFile.length();

        fullFile = new RandomAccessFile(originalFile, "r");
        pathToFileFragments = Paths.get(FRAGMENTS_PATH_NAME).resolve(fileName);

        int fragmentsNum = (int) fileSize / DEFAULT_FRAGMENT_SIZE_BYTES;

        for (int i = 0; i < fragmentsNum; i++) {
            byte[] data = new byte[DEFAULT_FRAGMENT_SIZE_BYTES];
            fullFile.read(data);
            final FileFragment fragment = new FileFragment(i, DEFAULT_FRAGMENT_SIZE_BYTES);
            availableFragments.put(i, fragment);
//            fragment.savePart();
        }

        int lastFragmentSize = (int) fileSize - fragmentsNum * DEFAULT_FRAGMENT_SIZE_BYTES;
        if (lastFragmentSize != 0) {
            numberOfFragments = fragmentsNum + 1;
            byte[] data = new byte[lastFragmentSize];
            fullFile.read(data);
            final FileFragment fragment = new FileFragment(fragmentsNum, lastFragmentSize);
            availableFragments.put(fragmentsNum, fragment);
//            fragment.savePart();
        } else {
            numberOfFragments = fragmentsNum;
        }
    }

    public FragmentedFile(int fileId, @NotNull String fileName, long fileSize) {
        this.fileId = fileId;
        this.fileName = fileName;
        this.fileSize = fileSize;

        pathToFileFragments = Paths.get(FRAGMENTS_PATH_NAME).resolve(fileName);
        int exactNumberOfParts = (int) fileSize / DEFAULT_FRAGMENT_SIZE_BYTES;
        if (exactNumberOfParts * DEFAULT_FRAGMENT_SIZE_BYTES != fileSize) {
            exactNumberOfParts += 1;
        }
        this.numberOfFragments = exactNumberOfParts;
        IntStream.iterate(0, i -> i + 1).limit(exactNumberOfParts).forEach(fragmentsIdsToDownload::add);
    }

    public int getFileId() {
        return fileId;
    }

    @NotNull
    public String getFileName() {
        return fileName;
    }

    @NotNull
    public Map<Integer, FileFragment> getAvailableFragments() {
        return availableFragments;
    }

    public int getNumberOfFragments() {
        return numberOfFragments;
    }

    public boolean allFragmentsDownloaded() {
        return availableFragments.size() == numberOfFragments;
    }

    public void downloadFragment(int fragmentId, byte[] content) throws IOException {
        new FileFragment(fragmentId, content.length).savePart(content);
    }

    public boolean hasFragment(int fragmentId) {
        return availableFragments.containsKey(fragmentId);
    }

    public int getNumberOfAvailableFragments() {
        return availableFragments.size();
    }

    @NotNull
    public FileFragment getFragment(int fragmentId) {
        return availableFragments.get(fragmentId);
    }

    @NotNull
    public byte[] getFragmentData(int fragmentId) throws IOException {
        return getFragment(fragmentId).getData();
    }

    public void saveNewFragment(int fragmentId, byte[] data) throws IOException {
        new FileFragment(fragmentId, data.length).savePart(data);
    }

    public void unionAllFragmentsToNewFile() throws IOException {
        final Path newFilePath = Paths.get(makeNewFileName());
        Files.createFile(newFilePath);
        for (Map.Entry<Integer, FileFragment> fragmentInfo : availableFragments.entrySet()) {
            fragmentInfo.getValue().writeToFileAndDeleteFragment(newFilePath);
        }
        availableFragments.clear();
    }

    @NotNull
    private String makeNewFileName() {
        String newFileName = fileName;
        int suffix = 1;
        while (Files.exists(Paths.get(newFileName))) {
            newFileName = getFileNameWithSuffix(fileName, suffix);
            suffix++;
        }
        return newFileName;
    }

    @NotNull
    private String getFileNameWithSuffix(@NotNull String fileName, int suffix) {
        final String[] fileNameParts = fileName.split("\\.");
        if (fileNameParts.length == 0) {
            return fileName + "_" + suffix;
        }
        final StringBuilder newFileNameBuilder = new StringBuilder();
        for (int i = 0; i < fileNameParts.length - 1; i++) {
            newFileNameBuilder.append(fileNameParts[i]);
        }
        newFileNameBuilder.append("_").append(suffix).append(".").append(fileNameParts[fileNameParts.length - 1]);
        return newFileNameBuilder.toString();
    }

    private class FileFragment {

        private final int fragmentId;
        private final int size;

        public FileFragment(int fragmentId, int size) {
            this.fragmentId = fragmentId;
            this.size = size;
        }

        public void savePart(byte[] data) throws IOException {
            if (Files.notExists(pathToFileFragments)) {
                Files.createDirectories(pathToFileFragments);
            }
            final Path fragmentPath = Files.createFile(pathToFileFragments.resolve(fragmentId + ".frag"));
            Files.write(fragmentPath, data);
            availableFragments.put(fragmentId, this);
            fragmentsIdsToDownload.remove(fragmentId);
        }

        public int getFragmentId() {
            return fragmentId;
        }

        public byte[] getData() throws IOException {
            return (fullFile == null ? getDataFromFragment() : getDataFromFile());
        }

        public int getSize() {
            return size;
        }

        public void writeToFile(@NotNull Path path) throws IOException {
            Files.write(path, getDataFromFragment(), StandardOpenOption.APPEND);
        }

        public void writeToFileAndDeleteFragment(@NotNull Path path) throws IOException {
            Files.write(path, getDataFromFragment(), StandardOpenOption.APPEND);
            Files.deleteIfExists(pathToFileFragments.resolve(fragmentId + ".frag"));
        }

        private byte[] getDataFromFragment() throws IOException {
            final Path fragmentPath = pathToFileFragments.resolve(fragmentId + ".frag");
            return Files.readAllBytes(fragmentPath);
        }

        private byte[] getDataFromFile() throws IOException {
            assert fullFile != null;
            byte[] data = new byte[size];
            fullFile.seek(fragmentId * DEFAULT_FRAGMENT_SIZE_BYTES);
            fullFile.read(data);
            return data;
        }
    }
}
