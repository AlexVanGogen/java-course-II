package ru.itmo.javacourse.torrent.interaction.filesystem.tracker;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import ru.itmo.javacourse.torrent.interaction.DistributorDescription;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

public class JsonTrackerMetadataWriter implements TrackerMetadataWriter {

    @Override
    public void writeDistributedFilesMetadata(@NotNull DistributedFilesMetadata metadata, @NotNull final FileWriter output) throws IOException {
        final Map<Integer, DistributedFilesMetadata.FileMeta> filesIdsAndDescriptions = metadata.getFilesIdsAndDescriptions();

        final JSONArray distributedFilesData = new JSONArray();

        for (Map.Entry<Integer, DistributedFilesMetadata.FileMeta> fileDescription : filesIdsAndDescriptions.entrySet()) {
            final JSONObject fileData = writeFileMetadata(fileDescription.getValue());
            fileData.put("id", fileDescription.getKey());
            distributedFilesData.put(fileData);
        }

        output.write(distributedFilesData.toString(4));
        output.flush();
    }

    @NotNull
    private JSONObject writeFileMetadata(@NotNull DistributedFilesMetadata.FileMeta metadata) throws IOException {

        final JSONObject fileData = new JSONObject();
        fileData.put("name", metadata.getFileName());
        fileData.put("size", metadata.getFileSize());

        final JSONArray distributorsData = new JSONArray();
        for (final DistributorDescription distributor : metadata.getDistributors()) {
            writeDistributorDescription(distributorsData, distributor);
        }

        fileData.put("distributors", distributorsData);
        return fileData;
    }

    private void writeDistributorDescription(@NotNull final JSONArray distributorsData, @NotNull final DistributorDescription distributor) {
        JSONObject distributorData = new JSONObject();
        writeIpAddress(distributor, distributorData);
        distributorData.put("port", distributor.getPort());
        distributorsData.put(distributorData);
    }

    private void writeIpAddress(@NotNull final DistributorDescription distributor, @NotNull final JSONObject distributorData) {
        final JSONArray ipData = new JSONArray(distributor.getAddress().getBytes());
        distributorData.put("ip", ipData);
    }
}