package ru.hse.spb.javacourse.git.entities;

import java.io.IOException;

public class RevisionNotFoundException extends IOException {
    public RevisionNotFoundException() { }

    public RevisionNotFoundException(String message) {
        super(message);
    }
}
