package ru.itmo.javacourse.torrent;

import java.io.IOException;

public class Cli {
    public static void main(String[] args) throws IOException {
        new Tracker().launch();
    }
}
