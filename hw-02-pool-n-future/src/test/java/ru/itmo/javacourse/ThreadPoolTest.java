package ru.itmo.javacourse;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.itmo.javacourse.future.LightExecutionException;
import ru.itmo.javacourse.future.LightFuture;
import ru.itmo.javacourse.pool.Pool;
import ru.itmo.javacourse.pool.ThreadPoolImpl;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ThreadPoolTest {

    private Pool threadPool;

    @NotNull private final Integer intConstant = 183;
    @NotNull private final String stringConstant = "abacaba";
    @NotNull private final Supplier<String> stringSupplier = () -> stringConstant;
    @NotNull private final Supplier<Integer> intSupplier = () -> intConstant;
    @NotNull private final Supplier<String> failingSupplier = () -> { throw new RuntimeException(stringConstant); };
    @NotNull private final Function<Integer, Integer> applicableIntFunction = i -> i + 1;
    @NotNull private final Function<String, String> applicableStringFunction = s -> ('a' <= s.charAt(0) && s.charAt(0) <= 'z') ? s.toUpperCase() : s.toLowerCase();
    @NotNull private final Function<String, String> failingFunction = message -> { throw new RuntimeException(message); };

    @AfterEach
    void shutdown() {
        threadPool.shutdown();
    }

    @Test
    @DisplayName("Inside the thread pool declared as `new ThreadPoolImpl(n)` exactly `n` threads must exist")
    void testNumberOfThreadsInPool() throws LightExecutionException {
        final Set<String> threadIds = new HashSet<>();
        final Set<LightFuture<String>> tasksReturningThreadsNames = new HashSet<>();
        final Supplier<String> threadNameSupplier = () -> Thread.currentThread().getName();
        final int[] numbersOfThreads = {1, 2, 4, 8, 16, 32, 42};

        for (final int numberOfThreads : numbersOfThreads) {
            threadPool = new ThreadPoolImpl(numberOfThreads);
            for (int i = 0; i < 100; i++) {
                tasksReturningThreadsNames.add(submitTaskWithInitialDelay(threadNameSupplier, 100));
            }

            for (final LightFuture<String> tasksReturningThreadsName : tasksReturningThreadsNames) {
                threadIds.add(tasksReturningThreadsName.get());
            }

            assertEquals(numberOfThreads, threadIds.size());

            threadPool.shutdown();
            threadIds.clear();
            tasksReturningThreadsNames.clear();
        }
    }

    @Test
    @DisplayName("If suppliers don't throw exceptions, all results must be received by pool")
    void testSingleTasksWithoutExceptions() throws LightExecutionException {
        threadPool = new ThreadPoolImpl(8);

        final List<LightFuture<String>> stringFutures = new ArrayList<>();
        final List<LightFuture<Integer>> intFutures = new ArrayList<>();

        final List<String> stringResults = new ArrayList<>();
        final List<Integer> intResults = new ArrayList<>();

        for (int i = 0; i < 50; i++) {
            stringFutures.add(submitTaskWithInitialDelay(stringSupplier, 100));
            intFutures.add(submitTaskWithInitialDelay(intSupplier, 100));
        }

        for (final LightFuture<String> future : stringFutures) {
            stringResults.add(future.get());
        }

        for (final LightFuture<Integer> future : intFutures) {
            intResults.add(future.get());
        }

        assertEquals(Stream.generate(stringSupplier).limit(50).collect(Collectors.toList()), stringResults);
        assertEquals(Stream.generate(intSupplier).limit(50).collect(Collectors.toList()), intResults);
    }

    @Test
    @DisplayName("Any task failed with exception must be detected")
    void testTaskThatThrowsException() {
        threadPool = new ThreadPoolImpl(8);

        final List<LightFuture<String>> stringFutures = new ArrayList<>();
        final List<String> stringResults = new ArrayList<>();
        final List<LightExecutionException> executionExceptions = new ArrayList<>();

        for (int i = 0; i < 50; i++) {
            stringFutures.add(submitTaskWithInitialDelay(stringSupplier, 10));
        }
        for (int i = 0; i < 10; i++) {
            stringFutures.add(submitTaskWithInitialDelay(failingSupplier, 10));
        }
        for (int i = 0; i < 50; i++) {
            stringFutures.add(submitTaskWithInitialDelay(stringSupplier, 10));
        }

        for (final LightFuture<String> future : stringFutures) {
            try {
                stringResults.add(future.get());
            } catch (LightExecutionException e) {
                executionExceptions.add(e);
            }
        }

        assertEquals(10, executionExceptions.size());
        assertEquals(Stream.generate(stringSupplier).limit(100).collect(Collectors.toList()), stringResults);
    }

    @Test
    @DisplayName("Any task failed with exception ends with `LightExecutionException` wrapper with base exception as a cause")
    void testLightExecutionExceptionHasCause() {
        threadPool = new ThreadPoolImpl(8);

        final LightFuture<String> futureResult = submitTaskWithInitialDelay(failingSupplier, 10);
        try {
            futureResult.get();
        } catch (LightExecutionException e) {
            assertEquals(RuntimeException.class, e.getCause().getClass());
            assertEquals(stringConstant, e.getCause().getMessage());
        }
    }

    @Test
    @DisplayName("Task is not done if it is not completed by the time")
    void testTaskStatusBeforeExecution() {
        threadPool = new ThreadPoolImpl(8);

        final LightFuture<String> futureResult = submitTaskWithInitialDelay(stringSupplier, 1000000);
        assertFalse(futureResult.isReady());
    }

    @Test
    @DisplayName("Task is done if it is completed successfully")
    void testTaskStatusAfterSuccessfulExecution() throws LightExecutionException {
        threadPool = new ThreadPoolImpl(8);

        final LightFuture<String> futureResult = submitTaskWithInitialDelay(stringSupplier, 1000);
        futureResult.get();
        assertTrue(futureResult.isReady());
    }

    @Test
    @DisplayName("Task is considered to be done if it is failed with exception")
    void testTaskStatusAfterFailedExecution() {
        threadPool = new ThreadPoolImpl(8);

        final LightFuture<String> futureResult = submitTaskWithInitialDelay(failingSupplier, 1000);
        try {
            futureResult.get();
        } catch (LightExecutionException ignored) {
        } finally {
            assertTrue(futureResult.isReady());
        }
    }

    @Test
    @DisplayName("Same tasks applied to the same result return the same value")
    void testTasksAppliedToTheSameFuture() throws LightExecutionException {
        threadPool = new ThreadPoolImpl(8);

        final List<LightFuture<Integer>> futuresChain = new ArrayList<>();
        final LightFuture<Integer> futureResult = submitTaskWithInitialDelay(intSupplier, 100);
        for (int i = 0; i < 100; i++) {
            futuresChain.add(futureResult.thenApply(applicableIntFunction));
        }

        final List<Integer> resultsChain = new ArrayList<>();
        for (final LightFuture<Integer> future : futuresChain) {
            resultsChain.add(future.get());
        }

        assertEquals(
                Stream.generate(
                        () -> applicableIntFunction.apply(intSupplier.get())
                ).limit(100).collect(Collectors.toList()),
                resultsChain
        );
    }

    @Test
    @DisplayName("Tasks that form composition work fine")
    void testCompositionOfTasks() throws LightExecutionException {
        threadPool = new ThreadPoolImpl(8);

        final List<LightFuture<Integer>> futuresChain = new ArrayList<>();
        futuresChain.add(submitTaskWithInitialDelay(intSupplier, 10));

        for (int i = 1; i < 100; i++) {
            futuresChain.add(futuresChain.get(i - 1).thenApply(applicableIntFunction));
        }

        final List<Integer> resultsChain = new ArrayList<>();
        for (final LightFuture<Integer> future : futuresChain) {
            resultsChain.add(future.get());
        }

        assertEquals(Stream.iterate(intConstant, i -> i + 1).limit(100).collect(Collectors.toList()), resultsChain);
    }

    @Test
    @DisplayName("Different chains that are submitted to the pool work fine")
    void testDifferentChains() throws LightExecutionException {
        threadPool = new ThreadPoolImpl(8);

        final List<LightFuture<Integer>> integerFuturesChain = new ArrayList<>();
        final List<LightFuture<String>> stringFuturesChain = new ArrayList<>();
        integerFuturesChain.add(submitTaskWithInitialDelay(intSupplier, 10));
        stringFuturesChain.add(submitTaskWithInitialDelay(stringSupplier, 11));
        for (int i = 1; i < 100; i++) {
            integerFuturesChain.add(integerFuturesChain.get(i - 1).thenApply(applicableIntFunction));
            stringFuturesChain.add(stringFuturesChain.get(i - 1).thenApply(applicableStringFunction));
        }

        final List<Integer> integerResultsChain = new ArrayList<>();
        for (final LightFuture<Integer> future : integerFuturesChain) {
            integerResultsChain.add(future.get());
        }

        final List<String> stringResultsChain = new ArrayList<>();
        for (final LightFuture<String> future : stringFuturesChain) {
            stringResultsChain.add(future.get());
        }

        assertEquals(Stream.iterate(intConstant, i -> i + 1).limit(100).collect(Collectors.toList()), integerResultsChain);
        assertEquals(
                Stream.iterate(
                        stringConstant,
                        s -> ('a' <= s.charAt(0) && s.charAt(0) <= 'z') ? s.toUpperCase() : s.toLowerCase()
                ).limit(100).collect(Collectors.toList()),
                stringResultsChain);
    }

    @Test
    @DisplayName("Exception thrown during the first task propagates to every task in chain")
    void testExceptionPassingThroughEntireChain() {
        threadPool = new ThreadPoolImpl(8);

        final LightFuture<String> futureResult = submitTaskWithInitialDelay(failingSupplier, 1000);
        final List<LightFuture<String>> futuresChain = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            futuresChain.add(futureResult.thenApply(applicableStringFunction));
        }

        try {
            futureResult.get();
        } catch (LightExecutionException ignored) {}

        final List<LightExecutionException> executionExceptions = new ArrayList<>();
        for (final LightFuture<String> future : futuresChain) {
            try {
                future.get();
            } catch (LightExecutionException e) {
                assertEquals(RuntimeException.class, e.getCause().getClass());
                assertEquals(stringConstant, e.getCause().getMessage());
                executionExceptions.add(e);
            }
        }

        assertEquals(100, executionExceptions.size());
        for (final LightExecutionException exception : executionExceptions) {
            assertEquals(RuntimeException.class, exception.getCause().getClass());
            assertEquals(stringConstant, exception.getCause().getMessage());
        }
    }

    @Test
    @DisplayName("Exception thrown during the current task propagates to the next tasks in chain")
    void testExceptionThrownInTheMiddleOfTheChain() {
        threadPool = new ThreadPoolImpl(8);

        final LightFuture<String> futureResult = submitTaskWithInitialDelay(stringSupplier, 100);
        final List<LightFuture<String>> futuresChain = new ArrayList<>();

        for (int i = 0; i < 50; i++) {
            futuresChain.add(futureResult.thenApply(applicableStringFunction));
        }
        final LightFuture<String> failingFuture = futureResult.thenApply(failingFunction);
        futuresChain.add(failingFuture);
        for (int i = 0; i < 50; i++) {
            futuresChain.add(failingFuture.thenApply(applicableStringFunction));
        }

        try {
            futureResult.get();
        } catch (LightExecutionException ignored) {}

        final List<LightExecutionException> executionExceptions = new ArrayList<>();
        final List<String> results = new ArrayList<>();

        for (final LightFuture<String> future : futuresChain) {
            try {
                results.add(future.get());
            } catch (LightExecutionException e) {
                assertEquals(RuntimeException.class, e.getCause().getClass());
                assertEquals(stringConstant, e.getCause().getMessage());
                executionExceptions.add(e);
            }
        }

        assertEquals(
                Stream.generate(
                        () -> applicableStringFunction.apply(stringSupplier.get())
                ).limit(50).collect(Collectors.toList()),
                results);

        assertEquals(51, executionExceptions.size());
    }

    private void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    private <T> LightFuture<T> submitTaskWithInitialDelay(@NotNull Supplier<T> supplier, long milliseconds) {
        return threadPool.submit(() -> { sleep(milliseconds); return supplier.get(); });
    }
}
