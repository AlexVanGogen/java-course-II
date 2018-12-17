package ru.itmo.javacourse.torrent;

import ru.itmo.javacourse.torrent.cli.Cli;
import ru.itmo.javacourse.torrent.cli.ClientCliException;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        Cli cli = new Cli();
        List<File> filesToUpload = cli.addFilesToUploadPrompt(scanner);
        cli.attachClient(new Client(1237, filesToUpload));

        while (true) {
            System.out.print("> ");

            String nextRequest = scanner.nextLine();

            try {
                cli.executeRequest(nextRequest);
            } catch (ClientCliException e) {
                cli.printHelp();
            }
        }
    }
}
