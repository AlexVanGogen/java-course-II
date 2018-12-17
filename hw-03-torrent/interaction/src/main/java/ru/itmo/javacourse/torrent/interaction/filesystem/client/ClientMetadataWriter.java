package ru.itmo.javacourse.torrent.interaction.filesystem.client;

import org.jetbrains.annotations.NotNull;
import ru.itmo.javacourse.torrent.interaction.filesystem.MetadataWriter;

import java.io.FileWriter;
import java.io.IOException;

public interface ClientMetadataWriter extends MetadataWriter {

    void writeDistributedFilesMetadata(@NotNull final DistributedFilesMetadata metadata, @NotNull final FileWriter output) throws IOException;
}
