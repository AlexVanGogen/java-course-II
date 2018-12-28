package ru.itmo.javacourse.pool;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.itmo.javacourse.future.LightExecutionException;
import ru.itmo.javacourse.future.LightFuture;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Simple pool with fixed threads number
 */
public class ThreadPoolImpl implements Pool {

    @NotNull
    private final Queue<LightFutureImpl<?>> tasksQueue;

    private final Thread[] workers;

    /**
     * Created threads and runs them.
     * @param numberOfThreads numbers of threads that must be created
     */
    public ThreadPoolImpl(int numberOfThreads) {
        tasksQueue = new LinkedList<>();
        workers = new ThreadWorker[numberOfThreads];
        for (int i = 0; i < numberOfThreads; i++) {
            workers[i] = new ThreadWorker();
            workers[i].start();
        }
    }

    /**
     * Adds given task to the pool. If there are waiting threads in the pool, one of them will start
     * executing that task. Otherwise, task will be added to the queue and wait until any thread
     * with accept it.
     * @param task task that must be executed
     * @param <T> type of result
     * @return a LightFuture representing completion of the task
     */
    @Override
    public @NotNull <T> LightFuture<T> submit(Supplier<T> task) {
        final LightFutureImpl<T> submittingTask = new LightFutureImpl<>(task);
        synchronized (tasksQueue) {
            tasksQueue.add(submittingTask);
            tasksQueue.notify();
        }
        return submittingTask;
    }

    /**
     * Immediately stops execution and stops every thread in the pool.
     */
    @Override
    public void shutdown() {
        for (final Thread worker : workers) {
            worker.interrupt();
        }
    }

    private class ThreadWorker extends Thread {

        @Override
        public void run() {
            LightFutureImpl task;
            try {
                while (!Thread.interrupted()) {
                    synchronized (tasksQueue) {
                        while (tasksQueue.isEmpty()) {
                            tasksQueue.wait();
                        }

                        task = tasksQueue.poll();
                    }
                    if (task != null) {
                        executeTask(task);
                    }
                }
            } catch (InterruptedException ignored) { }
        }

        /**
         * We need synchronization here by both events (execution done / execution failed with an exception)
         * to have possibility to notify threads that wait the result with {@link LightFutureImpl#get()}
         */
        private <T> void executeTask(LightFutureImpl<T> task) {
            try {
                synchronized (task) {
                    task.taskResult = task.taskSupplier.get();
                    onTaskExecutionFinish(task);
                    task.notifyAll();
                }
            } catch (Exception e) {
                synchronized (task) {
                    task.taskExecutionException = e;
                    onTaskExecutionFinish(task);
                    task.notifyAll();
                }
            }
        }

        private <T> void onTaskExecutionFinish(LightFutureImpl<T> task) {
            task.isDone = true;
            task.directlyAppliedTasks.forEach(task::submit);
            task.directlyAppliedTasks.clear();
        }
    }

    /**
     * Task for our custom thread pool
     */
    private class LightFutureImpl<T> implements LightFuture<T> {

        @NotNull private final Supplier<? extends T> taskSupplier;

        @Nullable private T taskResult;

        @Nullable private Throwable taskExecutionException;

        @NotNull private final List<LightFutureImpl<?>> directlyAppliedTasks;

        private boolean isDone;

        public LightFutureImpl(@NotNull Supplier<? extends T> supplier) {
            taskSupplier = supplier;
            isDone = false;
            directlyAppliedTasks = new ArrayList<>();
        }

        /**
         * Returns result of given task execution.
         * If it has not been calculated yet, waits for completion of task.
         * @return result of given task execution
         * @throws LightExecutionException if task execution failed with exception
         */
        @Nullable
        public synchronized T get() throws LightExecutionException {
            try {
                while (!isDone) {
                    wait();
                }
            } catch (InterruptedException e) {
                if (taskExecutionException != null) {
                    taskExecutionException.addSuppressed(e) ;
                } else {
                    taskExecutionException = e;
                }
            }

            if (taskExecutionException != null) {
                if (taskExecutionException instanceof ChainExecutionException) {
                    taskExecutionException = taskExecutionException.getCause();
                }
                throw new LightExecutionException(taskExecutionException);
            }
            return taskResult;
        }

        /**
         * Makes new task where given function is applied to the result of current task.
         * New task cannot be executed until current task will complete.
         * Method can be called multiple times. New tasks can be executed on any thread in the pool.
         * @param functionToApply function that will be applied to result in the next task
         * @param <R> result of next task execution
         * @return new task where given function is applied to the result of current task
         */
        @NotNull
        @Override
        public synchronized <R> LightFuture<R> thenApply(@NotNull Function<? super T, ? extends R> functionToApply) {
            final Supplier<R> nextTaskSupplier = () -> {
                try {
                    return functionToApply.apply(get());
                } catch (LightExecutionException e) {
                    // e.getCause() is the base exception the supplier threw.
                    // That exception wraps to special exception to distinguish situation when unknown exception
                    // comes right from that method, and we will need to unwrap it in `get()` method.
                    throw new ChainExecutionException(e.getCause());
                }
            };

            final LightFutureImpl<R> nextTask = new LightFutureImpl<>(nextTaskSupplier);

            if (isReady()) {
                submit(nextTask);
            } else {
                directlyAppliedTasks.add(nextTask);
            }
            return nextTask;
        }

        /**
         * @return true if given task is done
         */
        public synchronized boolean isReady() {
            return isDone;
        }

        private void submit(LightFutureImpl<?> task) {
            synchronized (tasksQueue) {
                tasksQueue.add(task);
                tasksQueue.notify();
            }
        }
    }

    private class ChainExecutionException extends RuntimeException {
        public ChainExecutionException(Throwable cause) {
            super(cause);
        }
    }
}
