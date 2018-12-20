package ru.itmo.javacourse.torrent.interaction.protocol;

import org.jetbrains.annotations.NotNull;
import ru.itmo.javacourse.torrent.interaction.Notifier;
import ru.itmo.javacourse.torrent.interaction.message.RequestType;
import ru.itmo.javacourse.torrent.interaction.message.client.ClientRequest;
import ru.itmo.javacourse.torrent.interaction.message.client.get.GetRequest;
import ru.itmo.javacourse.torrent.interaction.message.client.stat.StatRequest;
import ru.itmo.javacourse.torrent.interaction.message.tracker.TrackerRequest;
import ru.itmo.javacourse.torrent.interaction.message.tracker.list.ListRequest;
import ru.itmo.javacourse.torrent.interaction.message.tracker.sources.SourcesRequest;
import ru.itmo.javacourse.torrent.interaction.message.tracker.update.UpdateRequest;
import ru.itmo.javacourse.torrent.interaction.message.tracker.upload.UploadRequest;

import java.io.DataInputStream;
import java.io.IOException;

public class RequestProvider {

    public static ClientRequest getClientRequest(@NotNull final DataInputStream input) throws IOException {
        final byte requestId = input.readByte();

        switch (requestId) {
            case 1:
                return StatRequest.read(input);
            case 2:
                return GetRequest.read(input);
            default:
                throw new UnknownRequestIdException(requestId, RequestType.CLIENT);
        }
    }

    public static TrackerRequest getTrackerRequest(@NotNull final DataInputStream input) throws IOException {
        final byte requestId = input.readByte();

        switch (requestId) {
            case 1:
                return ListRequest.read(input);
            case 2:
                return UploadRequest.read(input);
            case 3:
                return SourcesRequest.read(input);
            case 4:
                return UpdateRequest.read(input);
            default:
                throw new UnknownRequestIdException(requestId, RequestType.TRACKER);
        }
    }
}
