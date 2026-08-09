package com.adb.scrcpy.connect.adb;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

public class AdbStream {
    private final AdbConnection connection;
    private final int localId;
    private int remoteId;
    private boolean closed = false;

    private final PipedOutputStream pipedOut = new PipedOutputStream();
    private final PipedInputStream pipedIn;

    public AdbStream(AdbConnection connection, int localId) throws Exception {
        this.connection = connection;
        this.localId = localId;
        this.pipedIn = new PipedInputStream(pipedOut, 65536);
    }

    public void setRemoteId(int remoteId) {
        this.remoteId = remoteId;
    }

    public int getLocalId() {
        return localId;
    }

    public int getRemoteId() {
        return remoteId;
    }

    public void write(byte[] data) throws Exception {
        if (closed) throw new Exception("Stream closed");
        connection.writeStream(localId, remoteId, data);
    }

    public void receive(byte[] data) throws Exception {
        if (data != null && data.length > 0) {
            pipedOut.write(data);
            pipedOut.flush();
        }
    }

    public InputStream getInputStream() {
        return pipedIn;
    }

    public void close() {
        if (!closed) {
            closed = true;
            try {
                pipedOut.close();
                connection.closeStream(localId, remoteId);
            } catch (Exception ignored) {}
        }
    }
}
