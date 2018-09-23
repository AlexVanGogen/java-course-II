package ru.hse.spb.javacourse.git;

import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class GitTest {

    private static final Path DATA_PATH = Paths.get("src/test/resources/ru/hse/spb/javacourse/git/data");
    private static final Path GIT_ROOT = Paths.get(".jgit");
    private static final Path BOOKS_TXT = DATA_PATH.resolve("books.txt");
    private static final Path LETTERS_TXT = DATA_PATH.resolve("letters.txt");
    private static final Path NUMBERS_TXT = DATA_PATH.resolve("numbers.txt");
    private static final Path WORD_PATH = DATA_PATH.resolve("word");
    private static final Path WORD_TXT = WORD_PATH.resolve("word.txt");
    private static final String BOOKS_CONTENTS = "Harry Potter";
    private static final String LETTERS_CONTENTS = "abacaba";
    private static final String NUMBERS_CONTENTS = "1234567890";
    private static final String WORD_CONTENTS = "kidding";
    private static final String NEW_WORD_CONTENTS = "bamboozled";
    private static final String COMMIT1_MESSAGE = "Add books.txt";
    private static final String COMMIT2_MESSAGE = "Add letters.txt";
    private static final String COMMIT3_MESSAGE = "Add numbers.txt and word/word.txt";
    private static final String COMMIT4_MESSAGE = "Change word/word.txt";

    @BeforeEach
    void initialize() throws IOException {
        testGitRepositoryInitializesOnlyOnce();
        Files.createDirectories(DATA_PATH);
        Files.createFile(BOOKS_TXT);
        Files.createFile(LETTERS_TXT);
        Files.createFile(NUMBERS_TXT);
        Files.createDirectory(WORD_PATH);
        Files.createFile(WORD_TXT);
        Files.write(BOOKS_TXT, Collections.singletonList(BOOKS_CONTENTS));
        Files.write(LETTERS_TXT, Collections.singletonList(LETTERS_CONTENTS));
        Files.write(NUMBERS_TXT, Collections.singletonList(NUMBERS_CONTENTS));
        Files.write(WORD_TXT, Collections.singletonList(WORD_CONTENTS));
    }

    @AfterEach
    void destroy() throws IOException {
        deleteDirectory(DATA_PATH);
        deleteDirectory(GIT_ROOT);
    }

    @Test
    private static void testGitRepositoryInitializesOnlyOnce() {
        assertDoesNotThrow(RepositoryManager::initialize);
        assertThrows(RepositoryAlreadyInitializedException.class, RepositoryManager::initialize);
    }

    @Test
    void testCommitAndLog() throws IOException {
        commit();
        assertEquals(WORD_CONTENTS, Files.lines(WORD_TXT).collect(Collectors.joining()));
        commitChange();
        assertEquals(NEW_WORD_CONTENTS, Files.lines(WORD_TXT).collect(Collectors.joining()));
        List<String> log = RepositoryManager.showLog();
        assertEquals(4, log.size());
        assertTrue(log.get(0).contains(COMMIT4_MESSAGE));
        assertTrue(log.get(0).endsWith("HEAD"));
        assertTrue(log.get(1).contains(COMMIT3_MESSAGE));
        assertTrue(log.get(2).contains(COMMIT2_MESSAGE));
        assertTrue(log.get(3).contains(COMMIT1_MESSAGE));
    }

    @Test
    void testCommitAndReset() throws IOException {
        commit();
        commitChange();
        List<String> log = RepositoryManager.showLog();
        String hashOfRevisionWithAddedNumbersAndWord = log.get(1).substring(0, 40);

        RepositoryManager.reset(hashOfRevisionWithAddedNumbersAndWord);
        log = RepositoryManager.showLog();
        assertEquals(3, log.size());
        assertTrue(log.get(0).contains(COMMIT3_MESSAGE));
        assertTrue(log.get(0).endsWith("HEAD"));
        assertTrue(log.get(1).contains(COMMIT2_MESSAGE));
        assertTrue(log.get(2).contains(COMMIT1_MESSAGE));
        assertEquals(WORD_CONTENTS, Files.lines(WORD_TXT).collect(Collectors.joining()));

        String hashOfRevisionWithAddedLetters = log.get(1).substring(0, 40);
        RepositoryManager.reset(hashOfRevisionWithAddedLetters);
        log = RepositoryManager.showLog();
        assertEquals(2, log.size());
        assertTrue(log.get(0).contains(COMMIT2_MESSAGE));
        assertTrue(log.get(0).endsWith("HEAD"));
        assertTrue(log.get(1).contains(COMMIT1_MESSAGE));
        assertTrue(Files.notExists(WORD_TXT));
        assertTrue(Files.notExists(NUMBERS_TXT));
    }

    @Test
    private void commit() {
        assertDoesNotThrow(
                () -> RepositoryManager.commit(
                        COMMIT1_MESSAGE,
                        Collections.singletonList(BOOKS_TXT.toString())
                )
        );
        assertDoesNotThrow(
                () -> RepositoryManager.commit(
                        COMMIT2_MESSAGE,
                        Collections.singletonList(LETTERS_TXT.toString())
                )
        );
        assertDoesNotThrow(
                () -> RepositoryManager.commit(
                        COMMIT3_MESSAGE,
                        Arrays.asList(NUMBERS_TXT.toString(), WORD_TXT.toString())
                )
        );
    }

    @Test
    private void commitChange() {
        assertDoesNotThrow(
                () -> {
                    Files.write(WORD_TXT, Collections.singletonList(NEW_WORD_CONTENTS));
                    RepositoryManager.commit(
                            COMMIT4_MESSAGE,
                            Collections.singletonList(WORD_TXT.toString())
                    );
                }
        );
    }

    private static void deleteDirectory(Path path) throws IOException {
        Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }
}
