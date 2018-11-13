package ru.hse.spb.javacourse.git.command;

import ru.hse.spb.javacourse.git.entities.CheckoutKind;
import ru.hse.spb.javacourse.git.entities.FileUnstager;
import ru.hse.spb.javacourse.git.entities.RepositoryManager;

import java.io.IOException;
import java.util.List;

public class Checkout extends GitCommand {

    @Override
    public String execute(List<String> args) throws IOException, GitCommandException {
        if (args.isEmpty()) {
            throw new GitCommandException();
        }
        super.execute(args);

        if (args.get(0).equals("--")) {
            List<String> filesToUnstage = args.subList(1, args.size());
            FileUnstager unstager = new FileUnstager(filesToUnstage);
            return unstager.unstage();
        } else {
            String revision = args.get(0);
            CheckoutKind kind = RepositoryManager.checkout(revision);
            return String.format("Checkout to %s %s", (kind == CheckoutKind.REVISION ? "revision" : "branch"), revision);
        }
    }
}
