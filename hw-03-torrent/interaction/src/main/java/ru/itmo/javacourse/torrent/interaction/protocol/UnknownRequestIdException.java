package ru.itmo.javacourse.torrent.interaction.protocol;

import ru.itmo.javacourse.torrent.interaction.message.RequestType;
import ru.itmo.javacourse.torrent.interaction.message.client.ClientRequest;
import ru.itmo.javacourse.torrent.interaction.message.tracker.TrackerRequest;

import java.io.IOException;

public class UnknownRequestIdException extends IOException {

    public UnknownRequestIdException(int requestId, RequestType requestType) {
        super(String.format(
                "Got request id %b, but %b is maximum possible",
                requestId,
                requestType == RequestType.CLIENT ? ClientRequest.MAX_ID : TrackerRequest.MAX_ID
        ));
    }
}
