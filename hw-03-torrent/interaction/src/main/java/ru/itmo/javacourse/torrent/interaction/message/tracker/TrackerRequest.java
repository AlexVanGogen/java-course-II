package ru.itmo.javacourse.torrent.interaction.message.tracker;

import ru.itmo.javacourse.torrent.interaction.Request;

public interface TrackerRequest extends Request {
    byte MAX_ID = 4;
}
