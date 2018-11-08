package ru.hse.spb.javacourse.git.command;


public class RepositoryInMergingStateException extends GitCommandException {
    public RepositoryInMergingStateException() {
        super("You have unmerged changes; resolve them and use \"./jgit.sh merge --continue to proceed\"");
    }
}
