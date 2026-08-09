package com.adb.scrcpy.connect.bt;

import android.bluetooth.BluetoothAdapter;import android.bluetooth.BluetoothDevice;import android.bluetooth.BluetoothServerSocket;import android.bluetooth.BluetoothSocket;import android.content.Context;import android.os.Handler;import android.os.Looper;

import java.io.InputStream;import java.io.OutputStream;import java.util.Set;import java.util.UUID;

public class BluetoothPairingManager {
    private static final String SERVICE_NAME = "WadbAutoPair";
    private static final UUID WADB_BT_UUID = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    private final Context context;
    private final BluetoothAdapter bluetoothAdapter;
    private BluetoothServerSocket serverSocket;
    private boolean isListening = false;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface BluetoothPairCallback {
        void onBluetoothPayloadReceived(String ip, int port);
    }

    public BluetoothPairingManager(Context context) {
        this.context = context;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public boolean isBluetoothAvailable() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    public void startBluetoothServer(final String ipPayload, final int portPayload) {
        if (!isBluetoothAvailable() || isListening) return;
        isListening = true;

        new Thread(() -> {
            try {
                serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(SERVICE_NAME, WADB_BT_UUID);
                while (isListening) {
                    try {
                        BluetoothSocket socket = serverSocket.accept();
                        OutputStream out = socket.getOutputStream();
                        String payload = ipPayload + ":" + portPayload + "\n";
                        out.write(payload.getBytes("UTF-8"));
                        out.flush();
                        socket.close();
                    } catch (Exception e) {
                        if (!isListening) break;
                    }
                }
            } catch (Exception ignored) {}
        }).start();
    }

    public void scanAndPairBluetoothDevice(final BluetoothPairCallback callback) {
        if (!isBluetoothAvailable()) return;

        new Thread(() -> {
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            if (pairedDevices == null || pairedDevices.isEmpty()) return;

            for (BluetoothDevice device : pairedDevices) {
                try {
                    BluetoothSocket socket = device.createRfcommSocketToServiceRecord(WADB_BT_UUID);
                    socket.connect();

                    InputStream in = socket.getInputStream();
                    byte[] buffer = new byte[256];
                    int read = in.read(buffer);
                    socket.close();

                    if (read > 0) {
                        String response = new String(buffer, 0, read, "UTF-8").trim();
                        if (response.contains(":")) {
                            String[] parts = response.split(":");
                            final String ip = parts[0];
                            final int port = Integer.parseInt(parts[1]);

                            mainHandler.post(() -> callback.onBluetoothPayloadReceived(ip, port));
                            break;
                        }
                    }
                } catch (Exception ignored) {}
            }
        }).start();
    }

    public void stop() {
        isListening = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (Exception ignored) {}
    }
}
