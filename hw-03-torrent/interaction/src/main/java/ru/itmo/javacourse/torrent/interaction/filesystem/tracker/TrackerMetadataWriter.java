package ru.itmo.javacourse.torrent.interaction.filesystem.tracker;

import org.jetbrains.annotations.NotNull;
import ru.itmo.javacourse.torrent.interaction.filesystem.MetadataWriter;

import java.io.FileWriter;
import java.io.IOException;

public interface TrackerMetadataWriter extends MetadataWriter {

    void writeDistributedFilesMetadata(@NotNull final DistributedFilesMetadata metadata, @NotNull final FileWriter output) throws IOException;
}
