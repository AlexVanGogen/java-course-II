package ru.hse.spb.javacourse.git.filestree;

import org.jetbrains.annotations.NotNull;
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
        for (Path nextCommitingFile : committingFilesPaths) {
            root.saveFile(nextCommitingFile);
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
                committedFiles.add(new Blob(nextBlobDeclaration));
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
