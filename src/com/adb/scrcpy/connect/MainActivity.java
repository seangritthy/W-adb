package com.adb.scrcpy.connect;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
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
import com.adb.scrcpy.connect.adb.AdbTlsPairing;
import com.adb.scrcpy.connect.bt.BluetoothPairingManager;
import com.adb.scrcpy.connect.crypto.AdbCrypto;
import com.adb.scrcpy.connect.scrcpy.ScrcpyController;
import com.adb.scrcpy.connect.share.ScreenReceiverClient;
import com.adb.scrcpy.connect.share.ScreenSenderServer;
import com.adb.scrcpy.connect.sync.AdbSyncClient;
import com.adb.scrcpy.connect.ui.FileExplorerAdapter;
import com.adb.scrcpy.connect.ui.RemoteScreenView;
import com.adb.scrcpy.connect.updater.AppUpdater;
import com.adb.scrcpy.connect.utils.QuickToolsController;
import com.adb.scrcpy.connect.wifi.AutoPairingEngine;
import com.adb.scrcpy.connect.wifi.HotspotPairingManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

public class MainActivity extends Activity {
    private static final int REQUEST_CODE_SCREEN_SHARE = 1001;

    private Button btnBluetoothAutoPair, btnZeroTouchAutoPair;
    private Button btnTabConnect, btnTabScrcpy, btnTabTerminal, btnTabFiles;
    private ScrollView panelConnect;
    private LinearLayout panelFiles, panelTerminal, cardDirectMode, cardRemoteMode, cardLocalMode;
    private RelativeLayout panelScrcpy;

    private Button btnModeDirectShare, btnModeRemote, btnModeLocal;
    private TextView tvMyIpAddress, tvAppVersion;
    private Button btnRefreshIp, btnStartSender, btnConnectDirectReceiver, btnAutoScanHotspot;

    private Button btnToolScreenshot, btnToolScreenOff, btnToolClipboard;

    private EditText etDirectIp, etIpAddress, etPort, etLocalPort, etLocalPairCode;
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
    private ScreenSenderServer screenSenderServer;
    private ScreenReceiverClient screenReceiverClient;

    private AppUpdater appUpdater;
    private HotspotPairingManager hotspotManager;
    private AutoPairingEngine autoPairingEngine;
    private BluetoothPairingManager bluetoothPairingManager;
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
        checkAutoUpdate();
    }

    private void initViews() {
        btnBluetoothAutoPair = findViewById(R.id.btnBluetoothAutoPair);
        btnZeroTouchAutoPair = findViewById(R.id.btnZeroTouchAutoPair);

        btnTabConnect = findViewById(R.id.btnTabConnect);
        btnTabScrcpy = findViewById(R.id.btnTabScrcpy);
        btnTabTerminal = findViewById(R.id.btnTabTerminal);
        btnTabFiles = findViewById(R.id.btnTabFiles);

        panelConnect = findViewById(R.id.panelConnect);
        panelScrcpy = findViewById(R.id.panelScrcpy);
        panelTerminal = findViewById(R.id.panelTerminal);
        panelFiles = findViewById(R.id.panelFiles);

        btnModeDirectShare = findViewById(R.id.btnModeDirectShare);
        btnModeRemote = findViewById(R.id.btnModeRemote);
        btnModeLocal = findViewById(R.id.btnModeLocal);

        cardDirectMode = findViewById(R.id.cardDirectMode);
        cardRemoteMode = findViewById(R.id.cardRemoteMode);
        cardLocalMode = findViewById(R.id.cardLocalMode);

        tvMyIpAddress = findViewById(R.id.tvMyIpAddress);
        tvAppVersion = findViewById(R.id.tvAppVersion);
        btnRefreshIp = findViewById(R.id.btnRefreshIp);
        btnStartSender = findViewById(R.id.btnStartSender);
        btnConnectDirectReceiver = findViewById(R.id.btnConnectDirectReceiver);
        btnAutoScanHotspot = findViewById(R.id.btnAutoScanHotspot);

        btnToolScreenshot = findViewById(R.id.btnToolScreenshot);
        btnToolScreenOff = findViewById(R.id.btnToolScreenOff);
        btnToolClipboard = findViewById(R.id.btnToolClipboard);

        etDirectIp = findViewById(R.id.etDirectIp);
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
        autoPairingEngine = new AutoPairingEngine(this);
        bluetoothPairingManager = new BluetoothPairingManager(this);
        screenReceiverClient = new ScreenReceiverClient();

        tvAppVersion.setText("v" + appUpdater.getInstalledVersionName() + " (20000)");
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
                    bluetoothPairingManager.startBluetoothServer(ip, 9090);
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
        btnBluetoothAutoPair.setOnClickListener(v -> triggerBluetoothAutoPair());
        btnZeroTouchAutoPair.setOnClickListener(v -> triggerZeroTouchAutoPair());

        btnTabConnect.setOnClickListener(v -> switchTab(0));
        btnTabScrcpy.setOnClickListener(v -> switchTab(1));
        btnTabTerminal.setOnClickListener(v -> switchTab(2));
        btnTabFiles.setOnClickListener(v -> switchTab(3));

        btnModeDirectShare.setOnClickListener(v -> toggleMode(0));
        btnModeRemote.setOnClickListener(v -> toggleMode(1));
        btnModeLocal.setOnClickListener(v -> toggleMode(2));

        btnRefreshIp.setOnClickListener(v -> detectMyIpAddress());
        btnStartSender.setOnClickListener(v -> requestScreenCapture());
        btnConnectDirectReceiver.setOnClickListener(v -> startDirectReceiverConnect());
        btnAutoScanHotspot.setOnClickListener(v -> autoScanHotspotDevice());

        btnToolScreenshot.setOnClickListener(v -> captureRemoteScreenshot());
        btnToolScreenOff.setOnClickListener(v -> turnOffRemoteScreen());
        btnToolClipboard.setOnClickListener(v -> sendClipboardText());

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

    private void captureRemoteScreenshot() {
        if (adbConnection == null || !adbConnection.isConnected()) {
            Toast.makeText(this, "Connect ADB first!", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                File shotFile = new File(getCacheDir(), "remote_screenshot_" + System.currentTimeMillis() + ".png");
                QuickToolsController.takeScreenshot(adbConnection, shotFile);
                mainHandler.post(() -> Toast.makeText(MainActivity.this, "📸 Screenshot saved to " + shotFile.getName(), Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                log("Screenshot Error: " + e.getMessage());
            }
        }).start();
    }

    private void turnOffRemoteScreen() {
        if (adbConnection == null || !adbConnection.isConnected()) {
            Toast.makeText(this, "Connect ADB first!", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                QuickToolsController.turnOffScreen(adbConnection);
                mainHandler.post(() -> Toast.makeText(MainActivity.this, "💡 Screen toggled!", Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                log("Screen Off Error: " + e.getMessage());
            }
        }).start();
    }

    private void sendClipboardText() {
        if (adbConnection == null || !adbConnection.isConnected()) {
            Toast.makeText(this, "Connect ADB first!", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                QuickToolsController.setClipboardText(adbConnection, "Hello from W-adb!");
                mainHandler.post(() -> Toast.makeText(MainActivity.this, "📋 Clipboard Sent!", Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                log("Clipboard Error: " + e.getMessage());
            }
        }).start();
    }

    private void triggerBluetoothAutoPair() {
        log("🔵 Scanning Bluetooth paired devices for W-adb...");
        Toast.makeText(this, "Scanning Bluetooth Devices...", Toast.LENGTH_SHORT).show();

        bluetoothPairingManager.scanAndPairBluetoothDevice((ip, port) -> {
            log("SUCCESS: Paired over Bluetooth! Target IP: " + ip + ":" + port);
            etDirectIp.setText(ip);
            etIpAddress.setText(ip);
            etPort.setText(String.valueOf(port));
            Toast.makeText(MainActivity.this, "Bluetooth Paired Device: " + ip + "! Connecting...", Toast.LENGTH_SHORT).show();

            if (port == 9090) {
                startDirectReceiverConnect();
            } else {
                startWifiConnectAndScrcpy();
            }
        });
    }

    private void triggerZeroTouchAutoPair() {
        log("⚡ ZERO-TOUCH AUTO PAIR: Scanning mDNS & Hotspot for nearby devices...");
        Toast.makeText(this, "⚡ Auto-Pairing Nearby Devices...", Toast.LENGTH_SHORT).show();

        autoPairingEngine.startZeroTouchAutoPair((ip, port, name) -> {
            log("Discovered Device over mDNS: " + ip + ":" + port + " (" + name + ")");
            etIpAddress.setText(ip);
            etPort.setText(String.valueOf(port));
            etDirectIp.setText(ip);
            Toast.makeText(MainActivity.this, "Auto-Paired Device: " + ip + "! Connecting...", Toast.LENGTH_SHORT).show();
            startWifiConnectAndScrcpy();
        });

        hotspotManager.discoverHotspotAdbDevice((ip, port) -> {
            if (ip != null && port > 0) {
                log("Discovered Hotspot ADB Device: " + ip + ":" + port);
                etIpAddress.setText(ip);
                etPort.setText(String.valueOf(port));
                etDirectIp.setText(ip);
                Toast.makeText(MainActivity.this, "Hotspot Auto-Paired: " + ip + "! Connecting...", Toast.LENGTH_SHORT).show();
                startWifiConnectAndScrcpy();
            }
        });
    }

    private void toggleMode(int modeIndex) {
        cardDirectMode.setVisibility(modeIndex == 0 ? View.VISIBLE : View.GONE);
        cardRemoteMode.setVisibility(modeIndex == 1 ? View.VISIBLE : View.GONE);
        cardLocalMode.setVisibility(modeIndex == 2 ? View.VISIBLE : View.GONE);

        btnModeDirectShare.setBackgroundTintList(android.content.res.ColorStateList.valueOf(modeIndex == 0 ? 0xFF00E676 : 0xFF2C2C2C));
        btnModeDirectShare.setTextColor(modeIndex == 0 ? 0xFF000000 : 0xFFFFFFFF);

        btnModeRemote.setBackgroundTintList(android.content.res.ColorStateList.valueOf(modeIndex == 1 ? 0xFF00E676 : 0xFF2C2C2C));
        btnModeRemote.setTextColor(modeIndex == 1 ? 0xFF000000 : 0xFFFFFFFF);

        btnModeLocal.setBackgroundTintList(android.content.res.ColorStateList.valueOf(modeIndex == 2 ? 0xFF29B6F6 : 0xFF2C2C2C));
        btnModeLocal.setTextColor(modeIndex == 2 ? 0xFF000000 : 0xFFFFFFFF);
    }

    private void requestScreenCapture() {
        MediaProjectionManager projectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if (projectionManager != null) {
            startActivityForResult(projectionManager.createScreenCaptureIntent(), REQUEST_CODE_SCREEN_SHARE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_SCREEN_SHARE && resultCode == RESULT_OK && data != null) {
            MediaProjectionManager projectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            if (projectionManager != null) {
                MediaProjection mediaProjection = projectionManager.getMediaProjection(resultCode, data);
                if (mediaProjection != null) {
                    new Thread(() -> {
                        try {
                            log("Starting Direct Screen Share Server on port 9090...");
                            screenSenderServer = new ScreenSenderServer(MainActivity.this, mediaProjection);
                            screenSenderServer.start();
                            log("SUCCESS: Screen Share Server Broadcasting! Tell User 1 to tap BLUETOOTH EASY AUTO-PAIR.");
                        } catch (Exception e) {
                            log("Sender Error: " + e.getMessage());
                        }
                    }).start();
                }
            }
        }
    }

    private void startDirectReceiverConnect() {
        String ip = etDirectIp.getText().toString().trim();
        if (ip.isEmpty()) {
            Toast.makeText(this, "Enter User 2's IP Address!", Toast.LENGTH_SHORT).show();
            return;
        }

        log("Connecting to Direct Wi-Fi Screen Share at " + ip + ":9090 (No ADB)...");

        new Thread(() -> {
            try {
                mainHandler.post(() -> switchTab(1));
                screenReceiverClient.connectAndStream(ip, 9090, remoteScreenView.getHolder().getSurface());
                log("SUCCESS: Direct Wi-Fi Screen Stream Active!");
            } catch (Exception e) {
                log("Direct Connection Failed: " + e.getMessage());
            }
        }).start();
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
                log("No active Hotspot ADB device found automatically.");
            }
        });
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
            }
        }).start();
    }

    private void startLocalLoopbackConnect() {
        String portStr = etLocalPort.getText().toString().trim();
        String pairCode = etLocalPairCode.getText().toString().trim();

        if (portStr.isEmpty()) {
            Toast.makeText(this, "Enter local Wireless Debugging port!", Toast.LENGTH_SHORT).show();
            return;
        }

        int port = Integer.parseInt(portStr);
        log("Connecting to Local Loopback (127.0.0.1:" + port + ")...");

        new Thread(() -> {
            try {
                if (!pairCode.isEmpty()) {
                    log("Attempting TLS pairing with code: " + pairCode + "...");
                    try {
                        AdbTlsPairing.pairDevice("127.0.0.1", port, pairCode);
                        log("TLS Pairing signal sent!");
                    } catch (Exception pErr) {
                        log("Pairing note: " + pErr.getMessage());
                    }
                }

                adbConnection = AdbConnection.createTcp("127.0.0.1", port, crypto);
                adbConnection.connect();
                log("SUCCESS: Local ADB Loopback Connected!");

                mainHandler.post(() -> {
                    Toast.makeText(MainActivity.this, "Local ADB Shell Connected!", Toast.LENGTH_SHORT).show();
                    switchTab(2);
                });
            } catch (Exception e) {
                log("Local ADB Failed: " + e.getMessage());
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
        if (bluetoothPairingManager != null) bluetoothPairingManager.stop();
        if (autoPairingEngine != null) autoPairingEngine.stopScan();
        if (screenSenderServer != null) screenSenderServer.stop();
        if (screenReceiverClient != null) screenReceiverClient.stop();
        if (scrcpyController != null) scrcpyController.stop();
        if (adbConnection != null) adbConnection.close();
    }
}
