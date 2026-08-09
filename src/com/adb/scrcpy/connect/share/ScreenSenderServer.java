package com.adb.scrcpy.connect.share;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.os.Handler;
import android.os.Looper;
import android.view.Surface;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class ScreenSenderServer {
    public static final int PORT = 9090;

    private final Context context;
    private final MediaProjection mediaProjection;
    private ServerSocket serverSocket;
    private MediaCodec encoder;
    private VirtualDisplay virtualDisplay;
    private boolean running = false;
    private final List<Socket> clientSockets = new ArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public ScreenSenderServer(Context context, MediaProjection mediaProjection) {
        this.context = context;
        this.mediaProjection = mediaProjection;
    }

    public void start() throws Exception {
        running = true;
        serverSocket = new ServerSocket(PORT);

        // Configure H.264 Encoder
        MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 1080, 1920);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, 2000000);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);

        encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        Surface inputSurface = encoder.createInputSurface();
        encoder.start();

        // Create Virtual Display for screen capture
        virtualDisplay = mediaProjection.createVirtualDisplay("W-adb-Stream", 1080, 1920, 320,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, inputSurface, null, null);

        // Start Accept Clients Loop Thread
        new Thread(() -> {
            while (running) {
                try {
                    Socket client = serverSocket.accept();
                    client.setTcpNoDelay(true);
                    synchronized (clientSockets) {
                        clientSockets.add(client);
                    }
                } catch (Exception e) {
                    if (!running) break;
                }
            }
        }).start();

        // Start Video Broadcast Loop Thread
        new Thread(this::broadcastVideoLoop).start();
    }

    private void broadcastVideoLoop() {
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        byte[] lenHeader = new byte[4];

        while (running) {
            try {
                int outIndex = encoder.dequeueOutputBuffer(bufferInfo, 10000);
                if (outIndex >= 0) {
                    ByteBuffer outBuffer = encoder.getOutputBuffer(outIndex);
                    if (outBuffer != null && bufferInfo.size > 0) {
                        byte[] chunk = new byte[bufferInfo.size];
                        outBuffer.get(chunk);

                        // Broadcast NAL frame to connected receiver clients
                        synchronized (clientSockets) {
                            List<Socket> closed = new ArrayList<>();
                            for (Socket client : clientSockets) {
                                try {
                                    OutputStream out = client.getOutputStream();
                                    // Send 4-byte chunk size header
                                    lenHeader[0] = (byte) ((bufferInfo.size >> 24) & 0xFF);
                                    lenHeader[1] = (byte) ((bufferInfo.size >> 16) & 0xFF);
                                    lenHeader[2] = (byte) ((bufferInfo.size >> 8) & 0xFF);
                                    lenHeader[3] = (byte) (bufferInfo.size & 0xFF);

                                    out.write(lenHeader);
                                    out.write(chunk);
                                    out.flush();
                                } catch (Exception e) {
                                    closed.add(client);
                                }
                            }
                            clientSockets.removeAll(closed);
                        }
                    }
                    encoder.releaseOutputBuffer(outIndex, false);
                }
            } catch (Exception e) {
                if (!running) break;
            }
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
            if (virtualDisplay != null) virtualDisplay.release();
            if (encoder != null) {
                encoder.stop();
                encoder.release();
            }
            if (mediaProjection != null) mediaProjection.stop();
        } catch (Exception ignored) {}
    }
}
