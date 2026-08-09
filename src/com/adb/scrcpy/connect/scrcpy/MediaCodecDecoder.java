package com.adb.scrcpy.connect.scrcpy;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.view.Surface;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class MediaCodecDecoder {
    private MediaCodec decoder;
    private Surface surface;
    private boolean running = false;
    private Thread decodeThread;

    public MediaCodecDecoder(Surface surface) {
        this.surface = surface;
    }

    public void start(InputStream videoStream) throws Exception {
        MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 1080, 1920);
        decoder = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
        decoder.configure(format, surface, null, 0);
        decoder.start();
        running = true;

        decodeThread = new Thread(() -> {
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            byte[] buffer = new byte[128 * 1024];

            try {
                while (running) {
                    int len = videoStream.read(buffer);
                    if (len <= 0) break;

                    int inputBufferIndex = decoder.dequeueInputBuffer(10000);
                    if (inputBufferIndex >= 0) {
                        ByteBuffer inputBuffer = decoder.getInputBuffer(inputBufferIndex);
                        if (inputBuffer != null) {
                            inputBuffer.clear();
                            inputBuffer.put(buffer, 0, len);
                            decoder.queueInputBuffer(inputBufferIndex, 0, len, System.currentTimeMillis() * 1000, 0);
                        }
                    }

                    int outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo, 0);
                    while (outputBufferIndex >= 0) {
                        decoder.releaseOutputBuffer(outputBufferIndex, true);
                        outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo, 0);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        decodeThread.start();
    }

    public void stop() {
        running = false;
        try {
            if (decoder != null) {
                decoder.stop();
                decoder.release();
            }
        } catch (Exception ignored) {}
    }
}
