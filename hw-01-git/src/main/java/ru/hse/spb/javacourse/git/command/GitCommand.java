package ru.hse.spb.javacourse.git.command;

import java.io.IOException;
import java.util.List;

public abstract class GitCommand {

    public abstract String execute(List<String> args) throws IOException, GitCommandException;
}
