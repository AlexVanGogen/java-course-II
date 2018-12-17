package ru.itmo.javacourse.torrent.interaction.message.client.stat;

import org.jetbrains.annotations.NotNull;
import ru.itmo.javacourse.torrent.interaction.message.client.ClientResponse;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class StatResponse implements ClientResponse {

    private final int availablePartsCount;
    @NotNull private final Collection<Integer> availablePartsIds;

    public StatResponse(int availablePartsCount, @NotNull Collection<Integer> availablePartsIds) {
        this.availablePartsCount = availablePartsCount;
        this.availablePartsIds = availablePartsIds;
    }

    @Override
    public void write(DataOutputStream output) throws IOException {
        output.writeInt(availablePartsCount);
        for (final int partId : availablePartsIds) {
            output.writeInt(partId);
        }
    }

    public static StatResponse read(@NotNull final DataInputStream input) throws IOException {
        final int availablePartsCount = input.readInt();
        final Collection<Integer> availablePartsIds = new ArrayList<>();
        for (int i = 0; i < availablePartsCount; i++) {
            final int nextPartId = input.readInt();
            availablePartsIds.add(nextPartId);
        }
        return new StatResponse(availablePartsCount, availablePartsIds);
    }

    public int getAvailablePartsCount() {
        return availablePartsCount;
    }

    @NotNull
    public Collection<Integer> getAvailablePartsIds() {
        return availablePartsIds;
    }
}
