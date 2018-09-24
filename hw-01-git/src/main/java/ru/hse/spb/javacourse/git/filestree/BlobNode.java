package ru.hse.spb.javacourse.git.filestree;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import ru.hse.spb.javacourse.git.Blob;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;

public final class BlobNode extends Node {

    private @NotNull Blob blob;
    private @NotNull CommitFilesTree root;

    public BlobNode(@NotNull Blob blob, @NotNull CommitFilesTree root) {
        this.blob = blob;
        this.path = blob.getObjectQualifiedPath();
        this.root = root;
        this.sha1 = blob.getSha1();
    }

    @Override
    String getHash() {
        return sha1;
    }

    @Override
    Path getPath() {
        return path;
    }

    @Override
    void fillSubtree() {}

    @Override
    void saveFile(Path file) {}

    @Override
    public void write(boolean saveBlob) throws IOException {
        Path file = Files.createFile(Paths.get(root.getRootDirectory().toString()).resolve(blob.getObjectQualifiedPath()));
        JSONObject blobInfo = new JSONObject();
        blobInfo.put("type", "blob");
        blobInfo.put("hash", blob.getSha1());
        blobInfo.put("path", blob.getObjectQualifiedPath().toString());
        Files.write(file, Collections.singletonList(blobInfo.toString()), StandardOpenOption.APPEND);
        if (saveBlob) {
            blob.save();
        }
    }

    @Override
    public void print() {
        System.out.println("blob " + blob.getSha1() + " " + blob.getObjectQualifiedPath().toString());
    }

    @NotNull
    public Blob getBlob() {
        return blob;
    }
}
