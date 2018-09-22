package ru.hse.spb.javacourse.git.filestree;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import ru.hse.spb.javacourse.git.Blob;
import ru.hse.spb.javacourse.git.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CommitFilesTree {

    private static final Path GIT_TREES_PATH = Paths.get(".jgit/trees/");

    private Node root;
    private Path rootDirectory;

    public CommitFilesTree(@NotNull String commitHash) throws IOException {
        Path treesPath = GIT_TREES_PATH;
        if (Files.notExists(treesPath)) {
            Files.createDirectory(treesPath);
        }
        rootDirectory = GIT_TREES_PATH.resolve(commitHash);
    }

    public void saveFiles(@NotNull List<Path> committingFilesPaths) throws IOException {
        Path currentPath = Paths.get("");
        root = new TreeNode(currentPath, this);
        for (Path nextCommittingFile : committingFilesPaths) {
            if (Files.notExists(nextCommittingFile)) {
                System.out.println("File not exists: " + nextCommittingFile.toString());
            }
            root.saveFile(nextCommittingFile);
        }
    }

    @NotNull
    public static List<Blob> getAllCommittedFiles(@NotNull Path filesTreePath) throws IOException {
        List<Path> textFilesPaths = Files.walk(filesTreePath)
                .filter(FileUtils::isTextFile)
                .collect(Collectors.toList());
        List<Blob> committedFiles = new ArrayList<>();
        for (Path nextFilePath: textFilesPaths) {
            List<String> declaredBlobs = Files.readAllLines(nextFilePath);
            for (String nextBlobDeclaration: declaredBlobs) {
                committedFiles.add(new Blob(new JSONObject(nextBlobDeclaration)));
            }
        }
        return committedFiles;
    }

    public void write() throws IOException {
        root.write();
    }

    @NotNull
    public Node getRoot() {
        return root;
    }

    @NotNull
    public Path getRootDirectory() {
        return rootDirectory;
    }

    @NotNull
    public String getHash() {
        return root.getHash();
    }
}
