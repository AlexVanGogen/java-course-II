package ru.hse.spb.javacourse.git.command;

import com.sun.org.apache.xml.internal.security.exceptions.Base64DecodingException;
import ru.hse.spb.javacourse.git.RepositoryManager;

import java.io.IOException;
import java.util.List;

public class Checkout extends GitCommand {

    @Override
    public String execute(List<String> args) throws IOException, GitCommandException {
        if (args.size() != 1) {
            throw new GitCommandException();
        }
        try {
            String revision = args.get(0);
            RepositoryManager.checkout(revision);
            return "Checkout to revision " + revision;
        } catch (Base64DecodingException e) {
            GitCommandException gitException = new GitCommandException();
            gitException.addSuppressed(e);
            throw gitException;
        }
    }
}
