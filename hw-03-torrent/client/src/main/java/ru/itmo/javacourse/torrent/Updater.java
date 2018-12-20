package ru.itmo.javacourse.torrent;

import org.jetbrains.annotations.NotNull;
import ru.itmo.javacourse.torrent.interaction.Notifier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static ru.itmo.javacourse.torrent.interaction.Configuration.*;

public class Updater {

    @NotNull private final ScheduledExecutorService updateScheduler;
    @NotNull private final Client client;
    @NotNull private final DistributedFilesManager manager;

    public Updater(@NotNull Client client, @NotNull DistributedFilesManager manager) {
        updateScheduler = new ScheduledThreadPoolExecutor(1);
        this.client = client;
        this.manager = manager;
    }

    public void launch() {
        updateScheduler.scheduleAtFixedRate(
                new UpdatingTask(),
                CLIENT_UPDATE_INITIAL_DELAY_SECS,
                CLIENT_UPDATE_PERIOD_SECS,
                TimeUnit.SECONDS
        );
    }

    private class UpdatingTask implements Runnable {
        @Override
        public void run() {
            try {
                Notifier.createClientMessage("Regular update procedure...");
                final Set<Integer> distributedFilesIds = manager.getDistributedFilesIds();
                client.executeUpdate(new ArrayList<>(distributedFilesIds));
            } catch (IOException e) {
                Notifier.createTrackerMessage("I/O error happened during updater lifecycle");
            }
        }
    }
}
