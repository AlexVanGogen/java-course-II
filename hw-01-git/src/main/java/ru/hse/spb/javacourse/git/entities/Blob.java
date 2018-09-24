package ru.hse.spb.javacourse.git.entities;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.*;
import java.util.Collections;
import java.util.stream.Collectors;

import static ru.hse.spb.javacourse.git.entities.RepositoryManager.GIT_INDEX_PATH;
import static ru.hse.spb.javacourse.git.entities.RepositoryManager.GIT_OBJECTS_PATH;

public class Blob {

    private @NotNull Path objectQualifiedPath;
    private @NotNull String sha1;
    private @NotNull String sha1Prefix;
    private @NotNull String sha1Suffix;

    private @NotNull String contentsEncoded;

    public Blob(@NotNull Path objectLocation) throws IOException {
        objectQualifiedPath = objectLocation;
        String contents = readContents(objectLocation);
        contentsEncoded = Base64.encodeBase64String(contents.getBytes());
        sha1 = DigestUtils.sha1Hex(contents);
        sha1Prefix = sha1.substring(0, 2);
        sha1Suffix = sha1.substring(2);
    }

    public Blob(@NotNull JSONObject blobDeclaration) throws IOException {
        sha1 = blobDeclaration.getString("hash");
        sha1Prefix = sha1.substring(0, 2);
        sha1Suffix = sha1.substring(2);
        objectQualifiedPath = Paths.get(blobDeclaration.getString("path"));
        contentsEncoded = getEncodedContentsForBlob(sha1);
    }

    public Blob(@NotNull String objectPath, @NotNull String hash) throws IOException {
        sha1 = hash;
        sha1Prefix = sha1.substring(0, 2);
        sha1Suffix = sha1.substring(2);
        objectQualifiedPath = Paths.get(objectPath);
        contentsEncoded = getEncodedContentsForBlob(sha1);
    }

    public void save() throws IOException {
        writeToObjects();
        writeToIndex();
    }

    public String decodeContents() {
        return new String(Base64.decodeBase64(contentsEncoded));
    }

    @NotNull
    public JSONObject toJson() {
        JSONObject blobData = new JSONObject();
        blobData.put("path", objectQualifiedPath.toString());
        blobData.put("hash", sha1);
        return blobData;
    }

    @NotNull
    public Path getObjectQualifiedPath() {
        return objectQualifiedPath;
    }

    @NotNull
    public String getSha1() {
        return sha1;
    }

    public String getEncodedContents() {
        return contentsEncoded;
    }

    private void writeToObjects() throws IOException {
        Path newObjectDirectory = GIT_OBJECTS_PATH.resolve(sha1Prefix);
        if (Files.notExists(newObjectDirectory))
            Files.createDirectory(newObjectDirectory);
        Path fileForNewObject = GIT_OBJECTS_PATH.resolve(sha1Prefix).resolve(sha1Suffix);
        if (Files.notExists(fileForNewObject)) {
            Files.createFile(fileForNewObject);
            Files.write(fileForNewObject, Collections.singletonList(contentsEncoded));
        }
    }

    private void writeToIndex() throws IOException {
        Files.write(GIT_INDEX_PATH, Collections.singletonList(toJson().toString()), StandardOpenOption.APPEND);
    }

    private String readContents(@NotNull Path location) throws IOException {
        return Files.lines(location).collect(Collectors.joining("\n"));
    }

    private String getEncodedContentsForBlob(@NotNull String hash) throws IOException {
        Path encodedContentsPath = GIT_OBJECTS_PATH.resolve(hash.substring(0, 2)).resolve(hash.substring(2));
        return String.join("\n", Files.readAllLines(encodedContentsPath));
    }
}
