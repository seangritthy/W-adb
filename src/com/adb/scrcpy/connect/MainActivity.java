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
import com.adb.scrcpy.connect.adb.AdbStream;
import com.adb.scrcpy.connect.crypto.AdbCrypto;
import com.adb.scrcpy.connect.scrcpy.ScrcpyController;
import com.adb.scrcpy.connect.sync.AdbSyncClient;
import com.adb.scrcpy.connect.ui.FileExplorerAdapter;
import com.adb.scrcpy.connect.ui.RemoteScreenView;
import com.adb.scrcpy.connect.updater.AppUpdater;
import com.adb.scrcpy.connect.wifi.HotspotPairingManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

public class MainActivity extends Activity {
    private Button btnTabConnect, btnTabScrcpy, btnTabTerminal, btnTabFiles;
    private ScrollView panelConnect;
    private LinearLayout panelFiles, panelTerminal, cardRemoteMode, cardLocalMode;
    private RelativeLayout panelScrcpy;

    private Button btnModeRemote, btnModeLocal;
    private TextView tvMyIpAddress;
    private Button btnRefreshIp, btnAutoScanHotspot;

    private EditText etIpAddress, etPort, etLocalPort, etLocalPairCode;
    private Button btnConnectWifi, btnConnectLocal;
    private TextView tvLogs;

    private RemoteScreenView remoteScreenView;
    private Button btnKeyBack, btnKeyHome, btnKeyRecents;

    private ScrollView scrollTerminal;
    private TextView tvTerminalOutput;
    private EditText etShellCommand;
    private Button btnSendShell;

    private TextView tvCurrentPath;
    private Button btnFileUp, btnPushFile;
    private ListView lvRemoteFiles;
    private FileExplorerAdapter fileAdapter;

    private AdbCrypto crypto;
    private AdbConnection adbConnection;
    private ScrcpyController scrcpyController;
    private AppUpdater appUpdater;
    private HotspotPairingManager hotspotManager;
    private String currentRemotePath = "/sdcard/";
    private boolean isLocalMode = false;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initCrypto();
        detectMyIpAddress();
        setupListeners();
        checkAutoUpdate();
    }

    private void initViews() {
        btnTabConnect = findViewById(R.id.btnTabConnect);
        btnTabScrcpy = findViewById(R.id.btnTabScrcpy);
        btnTabTerminal = findViewById(R.id.btnTabTerminal);
        btnTabFiles = findViewById(R.id.btnTabFiles);

        panelConnect = findViewById(R.id.panelConnect);
        panelScrcpy = findViewById(R.id.panelScrcpy);
        panelTerminal = findViewById(R.id.panelTerminal);
        panelFiles = findViewById(R.id.panelFiles);

        btnModeRemote = findViewById(R.id.btnModeRemote);
        btnModeLocal = findViewById(R.id.btnModeLocal);
        cardRemoteMode = findViewById(R.id.cardRemoteMode);
        cardLocalMode = findViewById(R.id.cardLocalMode);

        tvMyIpAddress = findViewById(R.id.tvMyIpAddress);
        btnRefreshIp = findViewById(R.id.btnRefreshIp);
        btnAutoScanHotspot = findViewById(R.id.btnAutoScanHotspot);

        etIpAddress = findViewById(R.id.etIpAddress);
        etPort = findViewById(R.id.etPort);
        etLocalPort = findViewById(R.id.etLocalPort);
        etLocalPairCode = findViewById(R.id.etLocalPairCode);

        btnConnectWifi = findViewById(R.id.btnConnectWifi);
        btnConnectLocal = findViewById(R.id.btnConnectLocal);
        tvLogs = findViewById(R.id.tvLogs);

        remoteScreenView = findViewById(R.id.remoteScreenView);
        btnKeyBack = findViewById(R.id.btnKeyBack);
        btnKeyHome = findViewById(R.id.btnKeyHome);
        btnKeyRecents = findViewById(R.id.btnKeyRecents);

        scrollTerminal = findViewById(R.id.scrollTerminal);
        tvTerminalOutput = findViewById(R.id.tvTerminalOutput);
        etShellCommand = findViewById(R.id.etShellCommand);
        btnSendShell = findViewById(R.id.btnSendShell);

        tvCurrentPath = findViewById(R.id.tvCurrentPath);
        btnFileUp = findViewById(R.id.btnFileUp);
        btnPushFile = findViewById(R.id.btnPushFile);
        lvRemoteFiles = findViewById(R.id.lvRemoteFiles);

        fileAdapter = new FileExplorerAdapter(this);
        lvRemoteFiles.setAdapter(fileAdapter);

        appUpdater = new AppUpdater(this);
        hotspotManager = new HotspotPairingManager(this);
    }

    private void initCrypto() {
        File keyFile = new File(getFilesDir(), "adb_rsa_key");
        new Thread(() -> {
            crypto = AdbCrypto.generateOrLoad(keyFile);
            log("ADB RSA Key loaded successfully.");
        }).start();
    }

    private void checkAutoUpdate() {
        appUpdater.checkForUpdates((hasUpdate, latestVersion, downloadUrl) -> {
            if (hasUpdate && downloadUrl != null && !downloadUrl.isEmpty()) {
                log("New update found: " + latestVersion);
                appUpdater.downloadAndInstall(downloadUrl);
            }
        });
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
        btnTabTerminal.setOnClickListener(v -> switchTab(2));
        btnTabFiles.setOnClickListener(v -> switchTab(3));

        btnModeRemote.setOnClickListener(v -> toggleMode(false));
        btnModeLocal.setOnClickListener(v -> toggleMode(true));

        btnRefreshIp.setOnClickListener(v -> detectMyIpAddress());
        btnAutoScanHotspot.setOnClickListener(v -> autoScanHotspotDevice());

        btnConnectWifi.setOnClickListener(v -> startWifiConnectAndScrcpy());
        btnConnectLocal.setOnClickListener(v -> startLocalLoopbackConnect());

        btnSendShell.setOnClickListener(v -> executeShellCommand());

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

    private void autoScanHotspotDevice() {
        log("Scanning Wi-Fi Hotspot for active ADB devices...");
        Toast.makeText(this, "Scanning Hotspot Network...", Toast.LENGTH_SHORT).show();

        hotspotManager.discoverHotspotAdbDevice((ip, port) -> {
            if (ip != null && port > 0) {
                log("FOUND Hotspot ADB Device: " + ip + ":" + port);
                etIpAddress.setText(ip);
                etPort.setText(String.valueOf(port));
                Toast.makeText(MainActivity.this, "Found Device: " + ip + ":" + port + "! Connecting...", Toast.LENGTH_SHORT).show();
                startWifiConnectAndScrcpy();
            } else {
                log("No active Hotspot ADB device found automatically. Please enter IP & Port manually.");
                Toast.makeText(MainActivity.this, "No ADB device found on Hotspot.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void toggleMode(boolean local) {
        isLocalMode = local;
        cardRemoteMode.setVisibility(local ? View.GONE : View.VISIBLE);
        cardLocalMode.setVisibility(local ? View.VISIBLE : View.GONE);

        btnModeRemote.setBackgroundTintList(android.content.res.ColorStateList.valueOf(local ? 0xFF2C2C2C : 0xFF00E676));
        btnModeRemote.setTextColor(local ? 0xFFFFFFFF : 0xFF000000);

        btnModeLocal.setBackgroundTintList(android.content.res.ColorStateList.valueOf(local ? 0xFF29B6F6 : 0xFF2C2C2C));
        btnModeLocal.setTextColor(local ? 0xFF000000 : 0xFFFFFFFF);
    }

    private void switchTab(int tabIndex) {
        panelConnect.setVisibility(tabIndex == 0 ? View.VISIBLE : View.GONE);
        panelScrcpy.setVisibility(tabIndex == 1 ? View.VISIBLE : View.GONE);
        panelTerminal.setVisibility(tabIndex == 2 ? View.VISIBLE : View.GONE);
        panelFiles.setVisibility(tabIndex == 3 ? View.VISIBLE : View.GONE);

        btnTabConnect.setTextColor(tabIndex == 0 ? 0xFF00E676 : 0xFFFFFFFF);
        btnTabScrcpy.setTextColor(tabIndex == 1 ? 0xFF00E676 : 0xFFFFFFFF);
        btnTabTerminal.setTextColor(tabIndex == 2 ? 0xFF00E676 : 0xFFFFFFFF);
        btnTabFiles.setTextColor(tabIndex == 3 ? 0xFF00E676 : 0xFFFFFFFF);

        if (tabIndex == 3 && adbConnection != null && adbConnection.isConnected()) {
            loadRemoteDirectory(currentRemotePath);
        }
    }

    private void startWifiConnectAndScrcpy() {
        String ip = etIpAddress.getText().toString().trim();
        String portStr = etPort.getText().toString().trim();

        if (ip.isEmpty() || portStr.isEmpty()) {
            Toast.makeText(this, "Please enter User 2's IP Address and Wireless Debugging Port!", Toast.LENGTH_SHORT).show();
            return;
        }

        int port = Integer.parseInt(portStr);
        log("Connecting to " + ip + ":" + port + "...");
        log("💡 Check target phone screen: If 'Allow Wireless Debugging?' prompt appears, tap ALLOW.");

        new Thread(() -> {
            try {
                adbConnection = AdbConnection.createTcp(ip, port, crypto);
                adbConnection.connect();
                log("SUCCESS: Connected to ADB!");

                mainHandler.post(() -> {
                    Toast.makeText(MainActivity.this, "ADB Connected! Starting Scrcpy...", Toast.LENGTH_SHORT).show();
                    initScrcpySession();
                });
            } catch (Exception e) {
                log("Connection Failed: " + e.getMessage());
                log("Troubleshooting:\n1. Ensure 'Wireless Debugging' is ON in Developer Options.\n2. Verify Port number (in Android 11+ it changes when re-toggled).\n3. Both phones must be on the same Wi-Fi/Hotspot network.");
            }
        }).start();
    }

    private void startLocalLoopbackConnect() {
        String portStr = etLocalPort.getText().toString().trim();
        if (portStr.isEmpty()) {
            Toast.makeText(this, "Enter local Wireless Debugging port!", Toast.LENGTH_SHORT).show();
            return;
        }

        int port = Integer.parseInt(portStr);
        log("Connecting to Local Loopback (127.0.0.1:" + port + ")...");
        log("💡 Check phone screen for 'Allow Wireless Debugging?' popup.");

        new Thread(() -> {
            try {
                adbConnection = AdbConnection.createTcp("127.0.0.1", port, crypto);
                adbConnection.connect();
                log("SUCCESS: Local ADB Loopback Connected!");

                mainHandler.post(() -> {
                    Toast.makeText(MainActivity.this, "Local ADB Shell Connected!", Toast.LENGTH_SHORT).show();
                    switchTab(2); // Switch to ADB Shell Terminal
                });
            } catch (Exception e) {
                log("Local ADB Failed: " + e.getMessage());
                log("Ensure 'Wireless Debugging' is enabled in Developer Options!");
            }
        }).start();
    }

    private void executeShellCommand() {
        String cmd = etShellCommand.getText().toString().trim();
        if (cmd.isEmpty()) return;

        if (adbConnection == null || !adbConnection.isConnected()) {
            Toast.makeText(this, "Connect ADB first!", Toast.LENGTH_SHORT).show();
            return;
        }

        etShellCommand.setText("");
        appendTerminal("$ " + cmd + "\n");

        new Thread(() -> {
            try {
                AdbStream stream = adbConnection.openStream("shell:" + cmd);
                InputStream in = stream.getInputStream();
                byte[] buf = new byte[4096];
                int read;
                while ((read = in.read(buf)) > 0) {
                    String output = new String(buf, 0, read, "UTF-8");
                    appendTerminal(output);
                }
                stream.close();
            } catch (Exception e) {
                appendTerminal("Command Error: " + e.getMessage() + "\n");
            }
        }).start();
    }

    private void appendTerminal(String text) {
        mainHandler.post(() -> {
            tvTerminalOutput.append(text);
            scrollTerminal.post(() -> scrollTerminal.fullScroll(View.FOCUS_DOWN));
        });
    }

    private void initScrcpySession() {
        new Thread(() -> {
            try {
                log("Deploying scrcpy-server to target device...");
                scrcpyController = new ScrcpyController(MainActivity.this, adbConnection);
                remoteScreenView.setController(scrcpyController);

                mainHandler.post(() -> switchTab(1));

                scrcpyController.startScrcpy(remoteScreenView.getHolder().getSurface());
                log("Scrcpy Live View Active!");
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
                fos.write("Hello from W-adb File Share!".getBytes());
                fos.close();

                String dest = currentRemotePath + "shared_file.txt";
                log("Uploading file to " + dest + "...");
                AdbSyncClient.pushFile(adbConnection, sampleFile, dest, null);
                log("File Uploaded Successfully!");
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
