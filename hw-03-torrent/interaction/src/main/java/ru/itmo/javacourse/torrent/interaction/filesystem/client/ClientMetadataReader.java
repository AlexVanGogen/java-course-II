package ru.itmo.javacourse.torrent.interaction.filesystem.client;

import org.jetbrains.annotations.NotNull;
import ru.itmo.javacourse.torrent.interaction.filesystem.MetadataReader;

import java.io.FileReader;
import java.io.IOException;

public interface ClientMetadataReader extends MetadataReader {

    @NotNull
    DistributedFilesMetadata readDistributedFilesMetadata(@NotNull final FileReader input) throws IOException;
}
