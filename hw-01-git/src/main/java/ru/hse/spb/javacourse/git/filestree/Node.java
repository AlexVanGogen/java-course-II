package ru.hse.spb.javacourse.git.filestree;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;

abstract class Node {

    protected String sha1 = null;
    protected Path path;

    abstract String getHash();
    abstract Path getPath();

    abstract void fillSubtree() throws IOException;

    abstract void saveFile(Path file) throws IOException;

    public abstract void write() throws IOException;
    public abstract void print();
}
