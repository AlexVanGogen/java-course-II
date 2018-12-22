package ru.itmo.javacourse.torrent.interaction.message.client.get;

import org.jetbrains.annotations.NotNull;
import ru.itmo.javacourse.torrent.interaction.message.client.ClientResponse;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class GetResponse implements ClientResponse {

    private final int contentSize;
    private final byte[] fragmentContent;

    public GetResponse(byte[] fragmentContent) {
        this.fragmentContent = fragmentContent;
        this.contentSize = fragmentContent.length;
    }

    @Override
    public void write(DataOutputStream output) throws IOException {
        output.writeInt(contentSize);
        output.write(fragmentContent);
    }

    public static GetResponse read(@NotNull final DataInputStream input) throws IOException {
        final int contentSize = input.readInt();
        final byte[] content = new byte[contentSize];
        final int bytesRead = input.read(content);
        if (bytesRead != contentSize) {
            throw new IOException();
        }
        return new GetResponse(content);
    }

    public byte[] getFragmentContent() {
        return fragmentContent;
    }
}
