package ru.hse.spb.javacourse.git;

import org.junit.jupiter.api.*;
import ru.hse.spb.javacourse.git.command.*;
import ru.hse.spb.javacourse.git.entities.RepositoryAlreadyInitializedException;
import ru.hse.spb.javacourse.git.entities.RepositoryManager;
import ru.hse.spb.javacourse.git.entities.Stage;
import ru.hse.spb.javacourse.git.filestatus.FileStatus;
import ru.hse.spb.javacourse.git.filestatus.StatusChecker;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class GitTest {

    private static final Path DATA_PATH = Paths.get("src/test/resources/ru/hse/spb/javacourse/git/data");
    private static final Path DATA_PATH_ROOT = Paths.get("src/test/resources/ru");
    private static final Path GIT_ROOT = Paths.get(".jgit");
    private static final Path BOOKS_TXT = DATA_PATH.resolve("books.txt");
    private static final Path LETTERS_TXT = DATA_PATH.resolve("letters.txt");
    private static final Path NUMBERS_TXT = DATA_PATH.resolve("numbers.txt");
    private static final Path WORD_PATH = DATA_PATH.resolve("word");
    private static final Path WORD_TXT = WORD_PATH.resolve("word.txt");
    private static final Path WORDS_TXT = WORD_PATH.resolve("words.txt");
    private static final String BOOKS_CONTENTS = "Harry Potter";
    private static final String LETTERS_CONTENTS = "abacaba";
    private static final String NUMBERS_CONTENTS = "1234567890";
    private static final String WORD_CONTENTS = "kidding";
    private static final String NEW_WORD_CONTENTS = "bamboozled";
    private static final String NEW_BOOK_CONTENTS = "Harry Potter and Java Memory Model";
    private static final String WORDS_CONTENTS = "lalala lalala";
    private static final String COMMIT1_MESSAGE = "Add books.txt";
    private static final String COMMIT2_MESSAGE = "Add letters.txt";
    private static final String COMMIT3_MESSAGE = "Add numbers.txt and word/word.txt";
    private static final String COMMIT4_MESSAGE = "Change word/word.txt";
    private static final String COMMIT5_MESSAGE = "Add word/words.txt";
    private static final String BRANCH1_NAME = "branch1";
    private static final String BRANCH2_NAME = "branch2";
    private static final String MASTER_BRANCH_NAME = "master";

    private StatusChecker statusChecker;

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
        statusChecker = new StatusChecker();
        statusChecker.getActualFileStates();
    }

    @AfterEach
    void destroy() throws IOException {
        deleteDirectory(DATA_PATH_ROOT);
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
        assertTrue(log.get(0).contains("HEAD"));
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
        assertTrue(log.get(0).contains("HEAD"));
        assertTrue(log.get(1).contains(COMMIT2_MESSAGE));
        assertTrue(log.get(2).contains(COMMIT1_MESSAGE));
        assertEquals(WORD_CONTENTS, Files.lines(WORD_TXT).collect(Collectors.joining()));

        String hashOfRevisionWithAddedLetters = log.get(1).substring(0, 40);
        RepositoryManager.reset(hashOfRevisionWithAddedLetters);
        log = RepositoryManager.showLog();
        assertEquals(2, log.size());
        assertTrue(log.get(0).contains(COMMIT2_MESSAGE));
        assertTrue(log.get(0).contains("HEAD"));
        assertTrue(log.get(1).contains(COMMIT1_MESSAGE));
        assertTrue(Files.notExists(WORD_TXT));
        assertTrue(Files.notExists(NUMBERS_TXT));
    }

    @Test
    void testStatusAfterCommits() throws IOException {
        assertEquals(FileStatus.MODIFIED, statusChecker.getState(BOOKS_TXT.toString()));
        assertEquals(FileStatus.MODIFIED, statusChecker.getState(LETTERS_TXT.toString()));
        assertEquals(FileStatus.MODIFIED, statusChecker.getState(NUMBERS_TXT.toString()));
        assertEquals(FileStatus.MODIFIED, statusChecker.getState(WORD_TXT.toString()));
        commit();
        statusChecker.getActualFileStates();
        assertEquals(FileStatus.UNCHANGED, statusChecker.getState(BOOKS_TXT.toString()));
        assertEquals(FileStatus.UNCHANGED, statusChecker.getState(LETTERS_TXT.toString()));
        assertEquals(FileStatus.UNCHANGED, statusChecker.getState(NUMBERS_TXT.toString()));
        assertEquals(FileStatus.UNCHANGED, statusChecker.getState(WORD_TXT.toString()));
        commitChange();
        statusChecker.getActualFileStates();
        assertEquals(FileStatus.UNCHANGED, statusChecker.getState(BOOKS_TXT.toString()));
        assertEquals(FileStatus.UNCHANGED, statusChecker.getState(LETTERS_TXT.toString()));
        assertEquals(FileStatus.UNCHANGED, statusChecker.getState(NUMBERS_TXT.toString()));
        assertEquals(FileStatus.UNCHANGED, statusChecker.getState(WORD_TXT.toString()));
    }

    @Test
    void testStatusAfterDeletion() throws IOException {
        commit();
        Files.deleteIfExists(BOOKS_TXT);
        assertTrue(Files.notExists(BOOKS_TXT));
        statusChecker.getActualFileStates();
        assertEquals(FileStatus.DELETED, statusChecker.getState(BOOKS_TXT.toString()));
        assertEquals(FileStatus.UNCHANGED, statusChecker.getState(LETTERS_TXT.toString()));
        assertEquals(FileStatus.UNCHANGED, statusChecker.getState(NUMBERS_TXT.toString()));
        assertEquals(FileStatus.UNCHANGED, statusChecker.getState(WORD_TXT.toString()));
    }

    @Test
    void testStatusAfterStaging() throws IOException {
        add();
        Stage stage = Stage.getStage();
        assertTrue(stage.fileInStage(BOOKS_TXT.toString()));
        assertFalse(stage.fileInStage(LETTERS_TXT.toString()));
        assertTrue(stage.fileInStage(NUMBERS_TXT.toString()));
        assertTrue(stage.fileInStage(WORD_TXT.toString()));

        statusChecker.getActualFileStates();
        assertEquals(FileStatus.STAGED, statusChecker.getState(BOOKS_TXT.toString()));
        assertEquals(FileStatus.MODIFIED, statusChecker.getState(LETTERS_TXT.toString()));
        assertEquals(FileStatus.STAGED, statusChecker.getState(NUMBERS_TXT.toString()));
        assertEquals(FileStatus.STAGED, statusChecker.getState(WORD_TXT.toString()));

        changeStagedFiles();
        statusChecker.getActualFileStates();
        assertEquals(FileStatus.MODIFIED, statusChecker.getState(BOOKS_TXT.toString()));
        assertEquals(FileStatus.MODIFIED, statusChecker.getState(LETTERS_TXT.toString()));
        assertEquals(FileStatus.STAGED, statusChecker.getState(NUMBERS_TXT.toString()));
        assertEquals(FileStatus.MODIFIED, statusChecker.getState(WORD_TXT.toString()));

        addChangedFiles();
        statusChecker.getActualFileStates();
        assertEquals(FileStatus.STAGED, statusChecker.getState(BOOKS_TXT.toString()));
        assertEquals(FileStatus.MODIFIED, statusChecker.getState(LETTERS_TXT.toString()));
        assertEquals(FileStatus.STAGED, statusChecker.getState(NUMBERS_TXT.toString()));
        assertEquals(FileStatus.STAGED, statusChecker.getState(WORD_TXT.toString()));
    }

    @Test
    void testCommitStagedFiles() throws IOException {
        add();
        assertDoesNotThrow(
                () -> {
                    new Commit().execute(Collections.singletonList("message"));
                }
        );
        statusChecker.getActualFileStates();
        assertEquals(FileStatus.UNCHANGED, statusChecker.getState(BOOKS_TXT.toString()));
        assertEquals(FileStatus.MODIFIED, statusChecker.getState(LETTERS_TXT.toString()));
        assertEquals(FileStatus.UNCHANGED, statusChecker.getState(NUMBERS_TXT.toString()));
        assertEquals(FileStatus.UNCHANGED, statusChecker.getState(WORD_TXT.toString()));

        List<String> log = RepositoryManager.showLog();
        assertEquals(1, log.size());
        assertTrue(log.get(0).contains("message"));

        Stage stage = Stage.getStage();
        assertFalse(stage.fileInStage(BOOKS_TXT.toString()));
        assertFalse(stage.fileInStage(LETTERS_TXT.toString()));
        assertFalse(stage.fileInStage(NUMBERS_TXT.toString()));
        assertFalse(stage.fileInStage(WORD_TXT.toString()));
    }

    @Test
    void testCommitAnyUnstagedAndStagedFiles() throws IOException {
        add();
        commit();
        statusChecker.getActualFileStates();

        assertEquals(FileStatus.UNCHANGED, statusChecker.getState(BOOKS_TXT.toString()));
        assertEquals(FileStatus.UNCHANGED, statusChecker.getState(LETTERS_TXT.toString()));
        assertEquals(FileStatus.UNCHANGED, statusChecker.getState(NUMBERS_TXT.toString()));
        assertEquals(FileStatus.UNCHANGED, statusChecker.getState(WORD_TXT.toString()));

        Stage stage = Stage.getStage();
        assertFalse(stage.fileInStage(BOOKS_TXT.toString()));
        assertFalse(stage.fileInStage(LETTERS_TXT.toString()));
        assertFalse(stage.fileInStage(NUMBERS_TXT.toString()));
        assertFalse(stage.fileInStage(WORD_TXT.toString()));
    }

    @Test
    void testAddAndCommitNonexistentFiles() throws IOException {
        assertTrue(new Add().execute(Collections.singletonList("nonexistent101.txt")).contains("File not found: nonexistent101.txt"));
        assertTrue(new Commit().execute(Arrays.asList("Add fake", "nonexistent101.txt")).contains("File not found: nonexistent101.txt"));
    }

    @Test
    void testUnstagingCheckoutOnUnstagedFile() throws IOException {
        commit();

        Files.write(BOOKS_TXT, Collections.singletonList(NEW_BOOK_CONTENTS));
        assertEquals(NEW_BOOK_CONTENTS, String.join("\n", Files.readAllLines(BOOKS_TXT)));
        assertDoesNotThrow(
                () -> new Checkout().execute(Arrays.asList("--", BOOKS_TXT.toString()))
        );
        statusChecker.getActualFileStates();
        assertEquals(FileStatus.UNCHANGED, statusChecker.getState(BOOKS_TXT.toString()));
        assertEquals(BOOKS_CONTENTS, String.join("\n", Files.readAllLines(BOOKS_TXT)));
    }

    @Test
    void testUnstagingCheckoutOnStagedFile() throws IOException {
        commit();

        Files.write(BOOKS_TXT, Collections.singletonList(NEW_BOOK_CONTENTS));
        assertDoesNotThrow(
                () -> new Add().execute(Collections.singletonList(BOOKS_TXT.toString()))
        );
        assertDoesNotThrow(
                () -> new Checkout().execute(Arrays.asList("--", BOOKS_TXT.toString()))
        );
        statusChecker.getActualFileStates();
        assertEquals(FileStatus.UNCHANGED, statusChecker.getState(BOOKS_TXT.toString()));
        assertEquals(BOOKS_CONTENTS, String.join("\n", Files.readAllLines(BOOKS_TXT)));
    }

    @Test
    void testCheckoutFastForwardBranches() throws IOException {
        commit();
        branch1();
        checkoutBranch1();
        assertEquals(WORD_CONTENTS, String.join("\n", Files.readAllLines(WORD_TXT)));
        commitChange();
        assertEquals(NEW_WORD_CONTENTS, String.join("\n", Files.readAllLines(WORD_TXT)));
        for (int i = 0; i < 10; i++) {
            checkoutMaster();
            assertEquals(WORD_CONTENTS, String.join("\n", Files.readAllLines(WORD_TXT)));
            checkoutBranch1();
            assertEquals(NEW_WORD_CONTENTS, String.join("\n", Files.readAllLines(WORD_TXT)));
        }
    }

    @Test
    void testCheckoutForkedBranches() throws IOException {
        commit1();
        commit2();
        branch1();
        checkoutBranch1();
        commit3();
        assertEquals(WORD_CONTENTS, String.join("\n", Files.readAllLines(WORD_TXT)));
        assertEquals(NUMBERS_CONTENTS, String.join("\n", Files.readAllLines(NUMBERS_TXT)));
        assertFalse(Files.exists(WORDS_TXT));
        checkoutMaster();
        branch2();
        checkoutBranch2();
        commitChange();
        Files.write(WORDS_TXT, Collections.singletonList(WORDS_CONTENTS));
        commit4();
        assertEquals(WORDS_CONTENTS, String.join("\n", Files.readAllLines(WORDS_TXT)));
        assertEquals(NEW_WORD_CONTENTS, String.join("\n", Files.readAllLines(WORD_TXT)));
        assertFalse(Files.exists(NUMBERS_TXT));
        for (int i = 0; i < 10; i++) {
            checkoutBranch1();
            assertEquals(WORD_CONTENTS, String.join("\n", Files.readAllLines(WORD_TXT)));
            assertEquals(NUMBERS_CONTENTS, String.join("\n", Files.readAllLines(NUMBERS_TXT)));
            assertFalse(Files.exists(WORDS_TXT));
            checkoutBranch2();
            assertEquals(WORDS_CONTENTS, String.join("\n", Files.readAllLines(WORDS_TXT)));
            assertEquals(NEW_WORD_CONTENTS, String.join("\n", Files.readAllLines(WORD_TXT)));
            assertFalse(Files.exists(NUMBERS_TXT));
        }
    }

    @Test
    void testFastForwardMerge() throws IOException {
        commit();
        branch1();
        checkoutBranch1();
        assertEquals(WORD_CONTENTS, String.join("\n", Files.readAllLines(WORD_TXT)));
        commitChange();
        assertEquals(NEW_WORD_CONTENTS, String.join("\n", Files.readAllLines(WORD_TXT)));
        checkoutMaster();
        assertEquals(WORD_CONTENTS, String.join("\n", Files.readAllLines(WORD_TXT)));
        mergeBranch1();
        assertEquals(NEW_WORD_CONTENTS, String.join("\n", Files.readAllLines(WORD_TXT)));
        List<String> log = RepositoryManager.showLog();
        assertEquals(4, log.size());
        assertTrue(log.get(0).contains(BRANCH1_NAME));
        assertTrue(log.get(0).contains(MASTER_BRANCH_NAME));
        assertTrue(log.get(0).contains("HEAD"));
    }

    @Test
    void testMergeWithConflicts() throws IOException {
        commit();
        branch1();
        checkoutBranch1();
        commitChange();
        checkoutMaster();
        branch2();
        checkoutBranch2();
        commitChange2();
        mergeBranch1();
        final List<String> strings = Files.readAllLines(WORD_TXT);
        assertEquals(5, strings.size());
        assertEquals("<<<<<<< HEAD", strings.get(0));
        assertEquals(NEW_WORD_CONTENTS + "!!!", strings.get(1));
        assertEquals("=======", strings.get(2));
        assertEquals(NEW_WORD_CONTENTS, strings.get(3));
        assertEquals(">>>>>>> " + BRANCH1_NAME, strings.get(4));
    }

    @Test
    void testMergeWithoutConflicts() throws IOException {
        commit1();
        commit2();
        branch1();
        checkoutBranch1();
        commit3();
        checkoutMaster();
        branch2();
        checkoutBranch2();
        commit4();
        assertFalse(Files.exists(NUMBERS_TXT));
        assertFalse(Files.exists(WORD_TXT));
        assertTrue(Files.exists(WORDS_TXT));
        mergeBranch1();
        assertTrue(Files.exists(NUMBERS_TXT));
        assertTrue(Files.exists(WORD_TXT));
        assertTrue(Files.exists(WORDS_TXT));
        assertEquals(WORD_CONTENTS, String.join("\n", Files.readAllLines(WORD_TXT)));
        assertEquals(NUMBERS_CONTENTS, String.join("\n", Files.readAllLines(NUMBERS_TXT)));
    }

    @Test
    private void commit() {
        commit1();
        commit2();
        commit3();
    }

    @Test
    private void commit1() {
        assertDoesNotThrow(
                () -> Main.execute(new String[]{
                        "commit",
                        COMMIT1_MESSAGE,
                        BOOKS_TXT.toString()}
                )
        );
    }

    @Test
    private void commit2() {
        assertDoesNotThrow(
                () -> Main.execute(new String[]{
                        "commit",
                        COMMIT2_MESSAGE,
                        LETTERS_TXT.toString()}
                )
        );
    }

    @Test
    private void commit3() {
        assertDoesNotThrow(
                () -> Main.execute(new String[]{
                        "commit",
                        COMMIT3_MESSAGE,
                        NUMBERS_TXT.toString(), WORD_TXT.toString()}
                )
        );
    }

    @Test
    private void commit4() {
        assertDoesNotThrow(
                () -> {
                    Files.write(WORDS_TXT, Collections.singletonList(WORDS_CONTENTS));
                    Main.execute(new String[]{
                            "commit",
                            COMMIT5_MESSAGE,
                            WORDS_TXT.toString()}
                    );
                }
        );
    }

    @Test
    private void branch1() {
        assertDoesNotThrow(
                () -> Main.execute(new String[]{
                        "branch",
                        BRANCH1_NAME}
                )
        );
    }

    @Test
    private void branch2() {
        assertDoesNotThrow(
                () -> Main.execute(new String[]{
                        "branch",
                        BRANCH2_NAME}
                )
        );
    }

    @Test
    private void checkoutBranch1() {
        assertDoesNotThrow(
                () -> Main.execute(new String[]{
                        "checkout",
                        BRANCH1_NAME}
                )
        );
    }

    @Test
    private void checkoutBranch2() {
        assertDoesNotThrow(
                () -> Main.execute(new String[]{
                        "checkout",
                        BRANCH2_NAME}
                )
        );
    }

    @Test
    private void checkoutMaster() {
        assertDoesNotThrow(
                () -> Main.execute(new String[]{
                        "checkout",
                        MASTER_BRANCH_NAME}
                )
        );
    }

    @Test
    private void mergeBranch1() {
        assertDoesNotThrow(
                () -> Main.execute(new String[]{
                        "merge",
                        BRANCH1_NAME}
                )
        );
    }

    @Test
    private void add() {
        assertDoesNotThrow(
                () -> new Add().execute(Collections.singletonList(BOOKS_TXT.toString()))
        );
        assertDoesNotThrow(
                () -> new Add().execute(Arrays.asList(NUMBERS_TXT.toString(), WORD_TXT.toString()))
        );
    }

    @Test
    private void commitChange() {
        assertDoesNotThrow(
                () -> {
                    Files.write(WORD_TXT, Collections.singletonList(NEW_WORD_CONTENTS));
                    Main.execute(new String[]{
                            "commit",
                            COMMIT4_MESSAGE,
                            WORD_TXT.toString()}
                    );
                }
        );
    }

    @Test
    private void commitChange2() {
        assertDoesNotThrow(
                () -> {
                    Files.write(WORD_TXT, Collections.singletonList(NEW_WORD_CONTENTS + "!!!"));
                    Main.execute(new String[]{
                            "commit",
                            COMMIT4_MESSAGE + "!!!",
                            WORD_TXT.toString()}
                    );
                }
        );
    }

    @Test
    private void changeStagedFiles() {
        assertDoesNotThrow(
                () -> {
                    Files.write(WORD_TXT, Collections.singletonList(NEW_WORD_CONTENTS));
                    Files.write(BOOKS_TXT, Collections.singletonList(NEW_BOOK_CONTENTS));
                }
        );
    }

    @Test
    private void addChangedFiles() {
        assertDoesNotThrow(
                () -> {
                    new Add().execute(Arrays.asList(WORD_TXT.toString(), BOOKS_TXT.toString()));
                }
        );
    }

    private static void deleteDirectory(Path path) throws IOException {
        Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        Files.deleteIfExists(path);
    }
}
