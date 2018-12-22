package ru.itmo.javacourse.torrent.interaction.message.tracker.sources;

import org.jetbrains.annotations.NotNull;
import ru.itmo.javacourse.torrent.interaction.DistributorDescription;
import ru.itmo.javacourse.torrent.interaction.IpAddress;
import ru.itmo.javacourse.torrent.interaction.message.tracker.TrackerResponse;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class SourcesResponse implements TrackerResponse {

    private final int distributorsCount;
    @NotNull private final Collection<DistributorDescription> distributorsDescriptions;

    public SourcesResponse(int distributorsCount, @NotNull Collection<DistributorDescription> distributorsDescriptions) {
        this.distributorsCount = distributorsCount;
        this.distributorsDescriptions = distributorsDescriptions;
    }

    @Override
    public void write(DataOutputStream output) throws IOException {
        output.writeInt(distributorsCount);
        for (@NotNull final DistributorDescription description : distributorsDescriptions) {
            description.getAddress().write(output);
            output.writeShort(description.getPort());
        }
    }

    public static SourcesResponse read(@NotNull final DataInputStream input) throws IOException {
        final int distributorsCount = input.readInt();
        final Collection<DistributorDescription> distributorsDescriptions = new ArrayList<>();
        for (int i = 0; i < distributorsCount; i++) {
            final IpAddress address = IpAddress.get(input);
            final short port = input.readShort();
            distributorsDescriptions.add(new DistributorDescription(address, port));
        }
        return new SourcesResponse(distributorsCount, distributorsDescriptions);
    }

    public int getDistributorsCount() {
        return distributorsCount;
    }

    @NotNull
    public Collection<DistributorDescription> getDistributorsDescriptions() {
        return distributorsDescriptions;
    }
}
