package ru.itmo.javacourse.torrent.interaction.message.client;

import ru.itmo.javacourse.torrent.interaction.SerializableRequest;

public interface ClientRequest extends SerializableRequest {
    byte MAX_ID = 2;
}
