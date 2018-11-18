package ru.itmo.javacourse.future;

/**
 * Wrapper for any exception thrown during pool execution.
 */
public class LightExecutionException extends Exception {
    public LightExecutionException(Throwable cause) {
        super(cause);
    }
}