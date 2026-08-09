package com.adb.scrcpy.connect;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.adb.scrcpy.connect.adb.AdbConnection;
import com.adb.scrcpy.connect.crypto.AdbCrypto;
import com.adb.scrcpy.connect.scrcpy.ScrcpyController;
import com.adb.scrcpy.connect.sync.AdbSyncClient;
import com.adb.scrcpy.connect.ui.FileExplorerAdapter;
import com.adb.scrcpy.connect.ui.RemoteScreenView;

import java.io.File;
import java.io.FileOutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

public class MainActivity extends Activity {
    private Button btnTabConnect, btnTabScrcpy, btnTabFiles;
    private ScrollView panelConnect;
    private LinearLayout panelFiles;
    private RelativeLayout panelScrcpy;

    private TextView tvMyIpAddress;
    private Button btnRefreshIp;

    private EditText etIpAddress, etPort, etPairCode;
    private Button btnConnectWifi;
    private TextView tvLogs;

    private RemoteScreenView remoteScreenView;
    private Button btnKeyBack, btnKeyHome, btnKeyRecents;

    private TextView tvCurrentPath;
    private Button btnFileUp, btnPushFile;
    private ListView lvRemoteFiles;
    private FileExplorerAdapter fileAdapter;

    private AdbCrypto crypto;
    private AdbConnection adbConnection;
    private ScrcpyController scrcpyController;
    private String currentRemotePath = "/sdcard/";

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initCrypto();
        detectMyIpAddress();
        setupListeners();
    }

    private void initViews() {
        btnTabConnect = findViewById(R.id.btnTabConnect);
        btnTabScrcpy = findViewById(R.id.btnTabScrcpy);
        btnTabFiles = findViewById(R.id.btnTabFiles);

        panelConnect = findViewById(R.id.panelConnect);
        panelScrcpy = findViewById(R.id.panelScrcpy);
        panelFiles = findViewById(R.id.panelFiles);

        tvMyIpAddress = findViewById(R.id.tvMyIpAddress);
        btnRefreshIp = findViewById(R.id.btnRefreshIp);

        etIpAddress = findViewById(R.id.etIpAddress);
        etPort = findViewById(R.id.etPort);
        etPairCode = findViewById(R.id.etPairCode);
        btnConnectWifi = findViewById(R.id.btnConnectWifi);
        tvLogs = findViewById(R.id.tvLogs);

        remoteScreenView = findViewById(R.id.remoteScreenView);
        btnKeyBack = findViewById(R.id.btnKeyBack);
        btnKeyHome = findViewById(R.id.btnKeyHome);
        btnKeyRecents = findViewById(R.id.btnKeyRecents);

        tvCurrentPath = findViewById(R.id.tvCurrentPath);
        btnFileUp = findViewById(R.id.btnFileUp);
        btnPushFile = findViewById(R.id.btnPushFile);
        lvRemoteFiles = findViewById(R.id.lvRemoteFiles);

        fileAdapter = new FileExplorerAdapter(this);
        lvRemoteFiles.setAdapter(fileAdapter);
    }

    private void initCrypto() {
        File keyFile = new File(getFilesDir(), "adb_rsa_key");
        new Thread(() -> {
            crypto = AdbCrypto.generateOrLoad(keyFile);
            log("ADB RSA Key loaded successfully.");
        }).start();
    }

    private void detectMyIpAddress() {
        new Thread(() -> {
            String ip = getLocalIpAddress();
            mainHandler.post(() -> {
                if (ip != null) {
                    tvMyIpAddress.setText(ip);
                } else {
                    tvMyIpAddress.setText("Offline / Hotspot not active");
                }
            });
        }).start();
    }

    private String getLocalIpAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                if (!intf.isUp() || intf.isLoopback()) continue;
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        boolean isIPv4 = sAddr.indexOf(':') < 0;
                        if (isIPv4) return sAddr;
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private void setupListeners() {
        btnTabConnect.setOnClickListener(v -> switchTab(0));
        btnTabScrcpy.setOnClickListener(v -> switchTab(1));
        btnTabFiles.setOnClickListener(v -> switchTab(2));

        btnRefreshIp.setOnClickListener(v -> detectMyIpAddress());

        btnConnectWifi.setOnClickListener(v -> startWifiConnectAndScrcpy());

        btnKeyBack.setOnClickListener(v -> sendKey(4));
        btnKeyHome.setOnClickListener(v -> sendKey(3));
        btnKeyRecents.setOnClickListener(v -> sendKey(187));

        btnFileUp.setOnClickListener(v -> navigateUpDirectory());
        btnPushFile.setOnClickListener(v -> uploadSampleFile());

        lvRemoteFiles.setOnItemClickListener((parent, view, position, id) -> {
            AdbSyncClient.FileItem item = fileAdapter.getItem(position);
            if (item.isDirectory) {
                String target = currentRemotePath.endsWith("/") ? currentRemotePath + item.name + "/" : currentRemotePath + "/" + item.name + "/";
                loadRemoteDirectory(target);
            } else {
                Toast.makeText(MainActivity.this, "File: " + item.name + " (" + item.size + " bytes)", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void switchTab(int tabIndex) {
        panelConnect.setVisibility(tabIndex == 0 ? View.VISIBLE : View.GONE);
        panelScrcpy.setVisibility(tabIndex == 1 ? View.VISIBLE : View.GONE);
        panelFiles.setVisibility(tabIndex == 2 ? View.VISIBLE : View.GONE);

        btnTabConnect.setTextColor(tabIndex == 0 ? 0xFF00E676 : 0xFFFFFFFF);
        btnTabScrcpy.setTextColor(tabIndex == 1 ? 0xFF00E676 : 0xFFFFFFFF);
        btnTabFiles.setTextColor(tabIndex == 2 ? 0xFF00E676 : 0xFFFFFFFF);

        if (tabIndex == 2 && adbConnection != null && adbConnection.isConnected()) {
            loadRemoteDirectory(currentRemotePath);
        }
    }

    private void startWifiConnectAndScrcpy() {
        String ip = etIpAddress.getText().toString().trim();
        String portStr = etPort.getText().toString().trim();
        String pairCode = etPairCode.getText().toString().trim();

        if (ip.isEmpty() || portStr.isEmpty()) {
            Toast.makeText(this, "Please enter User 2's IP Address and Wireless Debugging Port!", Toast.LENGTH_SHORT).show();
            return;
        }

        int port = Integer.parseInt(portStr);
        log("USER 1 -> Connecting to User 2 Target Device (" + ip + ":" + port + ")...");

        new Thread(() -> {
            try {
                adbConnection = AdbConnection.createTcp(ip, port, crypto);
                adbConnection.connect();
                log("SUCCESS: Connected to User 2 ADB!");

                mainHandler.post(() -> {
                    Toast.makeText(MainActivity.this, "ADB Paired & Connected! Starting Scrcpy...", Toast.LENGTH_SHORT).show();
                    initScrcpySession();
                });
            } catch (Exception e) {
                log("Connection Failed: " + e.getMessage());
                mainHandler.post(() -> {
                    Toast.makeText(MainActivity.this, "Connection Failed. Ensure Wireless Debugging is ON on User 2!", Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void initScrcpySession() {
        new Thread(() -> {
            try {
                log("Deploying scrcpy-server to User 2 device...");
                scrcpyController = new ScrcpyController(MainActivity.this, adbConnection);
                remoteScreenView.setController(scrcpyController);

                mainHandler.post(() -> switchTab(1));

                scrcpyController.startScrcpy(remoteScreenView.getHolder().getSurface());
                log("Scrcpy Live View Active! User 1 can now view & control User 2 screen.");
            } catch (Exception e) {
                log("Scrcpy Error: " + e.getMessage());
            }
        }).start();
    }

    private void sendKey(int keycode) {
        if (scrcpyController != null) {
            scrcpyController.sendKeycodeEvent(0, keycode, 0); // DOWN
            scrcpyController.sendKeycodeEvent(1, keycode, 0); // UP
        }
    }

    private void loadRemoteDirectory(String path) {
        log("Loading remote directory: " + path);
        new Thread(() -> {
            try {
                List<AdbSyncClient.FileItem> items = AdbSyncClient.listFiles(adbConnection, path);
                currentRemotePath = path;
                mainHandler.post(() -> {
                    tvCurrentPath.setText(path);
                    fileAdapter.setItems(items);
                });
            } catch (Exception e) {
                log("List files failed: " + e.getMessage());
            }
        }).start();
    }

    private void navigateUpDirectory() {
        if (!"/sdcard/".equals(currentRemotePath) && !"/".equals(currentRemotePath)) {
            String trimmed = currentRemotePath.substring(0, currentRemotePath.length() - 1);
            int lastSlash = trimmed.lastIndexOf("/");
            if (lastSlash >= 0) {
                String parent = trimmed.substring(0, lastSlash + 1);
                loadRemoteDirectory(parent);
            }
        }
    }

    private void uploadSampleFile() {
        if (adbConnection == null || !adbConnection.isConnected()) {
            Toast.makeText(this, "Connect ADB first!", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                File sampleFile = new File(getCacheDir(), "shared_file.txt");
                FileOutputStream fos = new FileOutputStream(sampleFile);
                fos.write("Hello from User 1 Scrcpy File Share!".getBytes());
                fos.close();

                String dest = currentRemotePath + "shared_file.txt";
                log("Uploading file to " + dest + "...");
                AdbSyncClient.pushFile(adbConnection, sampleFile, dest, null);
                log("File Uploaded to User 2 Device Successfully!");
                loadRemoteDirectory(currentRemotePath);
            } catch (Exception e) {
                log("Upload Error: " + e.getMessage());
            }
        }).start();
    }

    private void log(String message) {
        mainHandler.post(() -> {
            tvLogs.append(message + "\n");
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (scrcpyController != null) scrcpyController.stop();
        if (adbConnection != null) adbConnection.close();
    }
}
