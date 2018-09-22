package ru.hse.spb.javacourse.git;

import java.nio.file.Path;

public class FileUtils {

    public static boolean isTextFile(Path file) {
        return file.toString().endsWith(".txt");
    }
}
