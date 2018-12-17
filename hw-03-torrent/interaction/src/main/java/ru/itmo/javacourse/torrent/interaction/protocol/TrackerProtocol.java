package ru.itmo.javacourse.torrent.interaction.protocol;

import ru.itmo.javacourse.torrent.interaction.message.tracker.list.ListRequest;
import ru.itmo.javacourse.torrent.interaction.message.tracker.list.ListResponse;
import ru.itmo.javacourse.torrent.interaction.message.tracker.sources.SourcesRequest;
import ru.itmo.javacourse.torrent.interaction.message.tracker.sources.SourcesResponse;
import ru.itmo.javacourse.torrent.interaction.message.tracker.update.UpdateRequest;
import ru.itmo.javacourse.torrent.interaction.message.tracker.update.UpdateResponse;
import ru.itmo.javacourse.torrent.interaction.message.tracker.upload.UploadRequest;
import ru.itmo.javacourse.torrent.interaction.message.tracker.upload.UploadResponse;

public interface TrackerProtocol {

    ListResponse executeList(ListRequest request);

    SourcesResponse executeSources(SourcesRequest request);

    UpdateResponse executeUpdate(UpdateRequest request);

    UploadResponse executeUpload(UploadRequest request);
}
