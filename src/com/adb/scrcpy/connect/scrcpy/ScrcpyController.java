package com.adb.scrcpy.connect.scrcpy;

import android.content.Context;
import com.adb.scrcpy.connect.adb.AdbConnection;
import com.adb.scrcpy.connect.adb.AdbStream;
import com.adb.scrcpy.connect.sync.AdbSyncClient;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class ScrcpyController {
    private final AdbConnection connection;
    private final Context context;
    private AdbStream shellStream;
    private AdbStream videoStream;
    private AdbStream controlStream;
    private MediaCodecDecoder decoder;

    public ScrcpyController(Context context, AdbConnection connection) {
        this.context = context;
        this.connection = connection;
    }

    public void startScrcpy(android.view.Surface surface) throws Exception {
        // Step 1: Extract scrcpy-server jar asset to cache
        File localServerJar = new File(context.getCacheDir(), "scrcpy-server.jar");
        if (!localServerJar.exists()) {
            InputStream in = context.getAssets().open("scrcpy-server-v2.4.jar");
            FileOutputStream fos = new FileOutputStream(localServerJar);
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) > 0) {
                fos.write(buf, 0, r);
            }
            fos.close();
            in.close();
        }

        // Step 2: Push scrcpy-server.jar to /data/local/tmp/
        AdbSyncClient.pushFile(connection, localServerJar, "/data/local/tmp/scrcpy-server.jar", null);

        // Step 3: Run app_process shell command
        String cmd = "shell:CLASSPATH=/data/local/tmp/scrcpy-server.jar app_process / com.genymobile.scrcpy.Server 2.4 tunnel_forward=true control=true audio=false video_bit_rate=2000000 max_size=1080";
        shellStream = connection.openStream(cmd);

        // Allow server time to initialize sockets
        Thread.sleep(1000);

        // Step 4: Open video socket connection
        videoStream = connection.openStream("localabstract:scrcpy");

        // Step 5: Open control socket connection
        controlStream = connection.openStream("localabstract:scrcpy");

        // Read scrcpy header (device name + video dimensions)
        InputStream vIn = videoStream.getInputStream();
        byte[] dummyHeader = new byte[64];
        vIn.read(dummyHeader); // Skip device metadata header

        // Step 6: Start decoder
        decoder = new MediaCodecDecoder(surface);
        decoder.start(vIn);
    }

    public void sendTouchEvent(int action, long pointerId, int x, int y, int width, int height, float pressure) {
        if (controlStream != null) {
            try {
                byte[] packet = ScrcpyControlMessage.createTouchEvent(action, pointerId, x, y, width, height, pressure);
                controlStream.write(packet);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void sendKeycodeEvent(int action, int keycode, int metastate) {
        if (controlStream != null) {
            try {
                byte[] packet = ScrcpyControlMessage.createKeycodeEvent(action, keycode, metastate);
                controlStream.write(packet);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void stop() {
        if (decoder != null) decoder.stop();
        if (videoStream != null) videoStream.close();
        if (controlStream != null) controlStream.close();
        if (shellStream != null) shellStream.close();
    }
}
