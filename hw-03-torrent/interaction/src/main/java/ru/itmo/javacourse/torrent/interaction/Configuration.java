package ru.itmo.javacourse.torrent.interaction;


/**
 * All constants that are used across the project.
 * The purpose of storing all constants in one place is that it allows
 * to add possibility for client to set custom values easily.
 */
public class Configuration {

    public static final String DISTRIBUTED_FILES_METADATA_FILENAME = "files.meta";
    public static final short TRACKER_PORT = 8082;
    public static final int DEFAULT_FRAGMENT_SIZE_BYTES = 1024;
    public static final String TRACKER_ADDRESS = "127.0.0.1";

    public static final String TRACKER_FILES_META_PATH_NAME = ".jtorrent/tracker/";
    public static final String FRAGMENTS_PATH_NAME = ".jtorrent/client/fragments/";

}
