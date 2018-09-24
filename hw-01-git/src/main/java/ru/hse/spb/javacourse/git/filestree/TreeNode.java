package ru.hse.spb.javacourse.git.filestree;

import org.apache.commons.codec.digest.DigestUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;
import ru.hse.spb.javacourse.git.Blob;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class TreeNode extends Node {

    private List<Node> subNodes = new ArrayList<>();
    private CommitFilesTree root;

    public TreeNode(@NotNull Path path, @NotNull CommitFilesTree root) {
        this.path = path;
        this.root = root;
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
    void fillSubtree() throws IOException {
        List<Path> subdirectories = Files.list(path).collect(Collectors.toList());
        for (Path subpath : subdirectories) {
            Node node;
            if (Files.isDirectory(subpath))
                node = new TreeNode(subpath, root);
            else node = new BlobNode(new Blob(subpath), root);
            node.fillSubtree();
            subNodes.add(node);
        }
    }

    @Override
    public void saveFile(@NotNull Path file) throws IOException {
        Path firstDirectoryOnPath = file.subpath(0, 1);
        Path child = path.resolve(firstDirectoryOnPath);
        Node node = findNodeWithPath(child);
        boolean nodeExisted = node != null;
        if (Files.isDirectory(child)) {
            if (node == null) {
                node = new TreeNode(child, root);
            }
            Path nestedPath = firstDirectoryOnPath.relativize(file);
            node.saveFile(nestedPath);
        } else if (node == null) {
            node = new BlobNode(new Blob(child), root);
        }
        if (!nodeExisted) {
            subNodes.add(node);
        }
        sha1 = DigestUtils.sha1Hex(subNodes.stream().map(Node::getHash).collect(Collectors.joining()));
    }

    @Override
    public void write(boolean saveBlob) throws IOException {
        Files.createDirectory(root.getRootDirectory().resolve(path));
        for (Node node : subNodes) {
            node.write(saveBlob);
        }
        Path dataFilePath;
        if (path.getParent() == null)
            dataFilePath = root.getRootDirectory().resolve("tree_data");
        else
            dataFilePath = root.getRootDirectory().resolve(path.toString()).resolve("tree_data");
        Path file;
        if (Files.exists(dataFilePath))
            file = dataFilePath;
        else file = Files.createFile(dataFilePath);
        JSONObject subtreeData = new JSONObject();
        subtreeData.put("type", "tree");
        subtreeData.put("hash", getHash());
        subtreeData.put("path", path.toString());
        Files.write(file, Collections.singletonList(subtreeData.toString()), StandardOpenOption.APPEND);
    }

    @Override
    public void print() {
        for (Node node : subNodes)
            node.print();
        System.out.println("tree " + getHash() + " " + path.toString());
    }

    @Nullable
    private Node findNodeWithPath(@NotNull Path path) {
        for (Node node: subNodes) {
            if (node.getPath().equals(path))
                return node;
        }
        return null;
    }
}
