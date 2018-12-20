package ru.itmo.javacourse.torrent.interaction;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.LocalDateTime;

public class DistributorDescription {

    @NotNull private final IpAddress address;
    private final short port;
    private LocalDateTime lastUpdateTime;

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

    @NotNull
    public LocalDateTime getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(@NotNull LocalDateTime lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    public boolean isExpired() {
        final LocalDateTime currentTime = LocalDateTime.now();
        final long timeSinceLastUpdate = Duration.between(lastUpdateTime, currentTime).toMillis();
        boolean exp = (timeSinceLastUpdate >= Configuration.TRACKER_AWAIT_UPDATE_TIMEOUT_SECS * 1000);
        if (exp) {
            System.out.println(String.format("Client %s %d expired", address.toString(), port));
        }
        return exp;
    }
}
