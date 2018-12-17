package ru.itmo.javacourse.torrent.interaction.protocol;

import ru.itmo.javacourse.torrent.interaction.message.client.get.GetRequest;
import ru.itmo.javacourse.torrent.interaction.message.client.get.GetResponse;
import ru.itmo.javacourse.torrent.interaction.message.client.stat.StatRequest;
import ru.itmo.javacourse.torrent.interaction.message.client.stat.StatResponse;

public interface ClientProtocol {

    GetResponse executeGet(GetRequest request);

    StatResponse executeStat(StatRequest request);
}
