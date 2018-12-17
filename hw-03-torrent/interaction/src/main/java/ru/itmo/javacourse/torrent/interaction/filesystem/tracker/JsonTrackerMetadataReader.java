package ru.itmo.javacourse.torrent.interaction.filesystem.tracker;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import ru.itmo.javacourse.torrent.interaction.DistributorDescription;
import ru.itmo.javacourse.torrent.interaction.IpAddress;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JsonTrackerMetadataReader implements TrackerMetadataReader {

    @NotNull
    @Override
    public DistributedFilesMetadata readDistributedFilesMetadata(@NotNull final FileReader input) throws IOException {
        final Map<Integer, DistributedFilesMetadata.FileMeta> filesIdsAndDescriptions = new ConcurrentHashMap<>();

        try {
            final JSONArray filesData = new JSONArray(new JSONTokener(input));

            for (final Object fileData : filesData) {
                final DistributedFilesMetadata.FileMeta fileMeta = readFileMetadata((JSONObject) fileData);
                filesIdsAndDescriptions.put(fileMeta.getFileId(), fileMeta);
            }
        } catch (JSONException e) {
            System.out.println("Problems with JSON reading (probably file has inappropriate format)");
        }

        return new DistributedFilesMetadata(filesIdsAndDescriptions);
    }

    @NotNull
    private DistributedFilesMetadata.FileMeta readFileMetadata(@NotNull final JSONObject fileData) throws IOException {
        final int fileId = fileData.getInt("id");
        final String fileName = fileData.getString("name");
        final long fileSize = fileData.getLong("size");
        final List<DistributorDescription> fileDistributors = new ArrayList<>();

        final JSONArray distributorsData = fileData.getJSONArray("distributors");
        for (Object distributorData : distributorsData) {
            fileDistributors.add(readDistributorDescription((JSONObject) distributorData));
        }

        return new DistributedFilesMetadata.FileMeta(fileId, fileName, fileSize, fileDistributors);
    }

    @NotNull
    private DistributorDescription readDistributorDescription(@NotNull final JSONObject distributorData) throws IOException {
        final JSONArray ipData = distributorData.getJSONArray("ip");
        final IpAddress address = readIpAddress(ipData);
        final int port = distributorData.getInt("port");
        return new DistributorDescription(address, port);
    }

    @NotNull
    private IpAddress readIpAddress(@NotNull final JSONArray ipData) throws IOException {
        final byte[] ip = new byte[4];
        for (int i = 0; i < 4; i++) {
            ip[i] = (byte) ipData.getInt(i);
        }
        return new IpAddress(ip);
    }
}
