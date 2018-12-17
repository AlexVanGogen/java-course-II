package ru.itmo.javacourse.torrent.interaction.message.tracker;

import ru.itmo.javacourse.torrent.interaction.SerializableRequest;

public interface TrackerRequest extends SerializableRequest {
    byte MAX_ID = 4;
}
