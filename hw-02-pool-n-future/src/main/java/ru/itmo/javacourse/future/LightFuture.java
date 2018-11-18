package ru.itmo.javacourse.future;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * Task for our custom thread pool.
 */
public interface LightFuture<T> {

    /**
     * Returns result of given task execution.
     * If it has not been calculated yet, waits for completion of task.
     * @return result of given task execution
     * @throws LightExecutionException if task execution failed with exception
     */
    @Nullable
    T get() throws LightExecutionException;

    /**
     * Makes new task where given function is applied to the result of current task.
     * New task cannot be executed until current task will complete.
     * Method can be called multiple times. New tasks can be executed on any thread in the pool.
     * @param functionToApply function that will be applied to result in the next task
     * @param <R> result of next task execution
     * @return new task where given function is applied to the result of current task
     */
    @NotNull
    <R> LightFuture<R> thenApply(Function<? super T, ? extends R> functionToApply);

    /**
     * @return true if given task is done
     */
    boolean isReady();
}
