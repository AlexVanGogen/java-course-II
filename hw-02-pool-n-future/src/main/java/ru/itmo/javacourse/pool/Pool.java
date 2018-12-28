package ru.itmo.javacourse.pool;

import org.jetbrains.annotations.NotNull;
import ru.itmo.javacourse.future.LightFuture;

import java.util.function.Supplier;

/**
 * Simple pool with fixed threads number
 */
public interface Pool {

    /**
     * Adds given task to the pool. If there are waiting threads in the pool, one of them will start
     * executing that task. Otherwise, task will be added to the queue and wait until any thread
     * with accept it.
     * @param task task that must be executed
     * @param <T> type of result
     * @return a LightFuture representing completion of the task
     */
    @NotNull
    <T> LightFuture<T> submit(Supplier<T> task);

    /**
     * Immediately stops execution and stops every thread in the pool.
     */
    void shutdown();
}
