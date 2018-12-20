package ru.itmo.javacourse.torrent.interaction;

import org.jetbrains.annotations.NotNull;

public class DistributorDescription {

    @NotNull private final IpAddress address;
    private final short port;

    public DistributorDescription(@NotNull IpAddress address, short port) {
        this.address = address;
        this.port = port;
    }

    @NotNull
    public IpAddress getAddress() {
        return address;
    }

    public short getPort() {
        return port;
    }
}
