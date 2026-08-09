package com.adb.scrcpy.connect.share;

import android.view.Surface;
import com.adb.scrcpy.connect.scrcpy.MediaCodecDecoder;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public class ScreenReceiverClient {
    private Socket socket;
    private MediaCodecDecoder decoder;
    private boolean running = false;

    public void connectAndStream(String host, int port, Surface surface) throws Exception {
        socket = new Socket();
        socket.setTcpNoDelay(true);
        socket.connect(new InetSocketAddress(host, port), 5000);

        InputStream in = socket.getInputStream();
        running = true;

        decoder = new MediaCodecDecoder(surface);
        decoder.start(in);
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    public void stop() {
        running = false;
        try {
            if (decoder != null) decoder.stop();
            if (socket != null) socket.close();
        } catch (Exception ignored) {}
    }
}
