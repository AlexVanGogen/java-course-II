package ru.itmo.javacourse.torrent.interaction;

import org.jetbrains.annotations.NotNull;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;

public class IpAddress implements Serializable {
    private static final byte IP_BYTES = 4;

    private final byte[] ip;

    public IpAddress(byte[] ip) {
        this.ip = ip;
    }

    public static IpAddress get(@NotNull final DataInputStream input) throws IOException {
        final byte[] ip = new byte[IP_BYTES];
        final int bytesRead = input.read(ip);
        if (bytesRead != IP_BYTES) {
            throw new IOException();
        }
        return new IpAddress(ip);
    }

    public byte[] getBytes() {
        return ip;
    }

    public void write(@NotNull final DataOutputStream output) throws IOException {
        output.write(ip);
    }

    @Override
    public String toString() {
        return String.format("%d.%d.%d.%d", toPositive(ip[0]), toPositive(ip[1]), toPositive(ip[2]), toPositive(ip[3]));
    }

    private int toPositive(byte value) {
        if (value < 0) {
            return value + 128;
        }
        return value;
    }
}
