package ru.hse.spb.javacourse.git;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

public class FileUtils {

    public static boolean isTextFile(Path file) {
        return file.toString().endsWith(".txt");
    }

    public static void deleteDirectory(Path path) throws IOException {
        Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }
}
