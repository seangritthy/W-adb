package com.adb.scrcpy.connect.wifi;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;

import java.io.BufferedReader;
import java.io.FileReader;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class HotspotPairingManager {
    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public HotspotPairingManager(Context context) {
        this.context = context;
    }

    public static String getHotspotGatewayIp(Context context) {
        try {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                DhcpInfo dhcp = wifiManager.getDhcpInfo();
                if (dhcp != null && dhcp.gateway != 0) {
                    return (dhcp.gateway & 0xFF) + "." +
                           ((dhcp.gateway >> 8) & 0xFF) + "." +
                           ((dhcp.gateway >> 16) & 0xFF) + "." +
                           ((dhcp.gateway >> 24) & 0xFF);
                }
            }
        } catch (Exception ignored) {}
        return "192.168.43.1"; // Default Android Mobile Hotspot Gateway IP
    }

    public static List<String> getConnectedHotspotClients() {
        List<String> ips = new ArrayList<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader("/proc/net/arp"));
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\\s+");
                if (parts.length >= 4) {
                    String ip = parts[0];
                    String flags = parts[2];
                    if (!"0x0".equals(flags) && !ip.equals("IP") && !ip.startsWith("127.")) {
                        ips.add(ip);
                    }
                }
            }
            br.close();
        } catch (Exception ignored) {}
        return ips;
    }

    public void discoverHotspotAdbDevice(final DiscoveryCallback callback) {
        new Thread(() -> {
            List<String> targetIps = getConnectedHotspotClients();
            String gatewayIp = getHotspotGatewayIp(context);
            if (!targetIps.contains(gatewayIp)) {
                targetIps.add(gatewayIp);
            }

            int[] commonPorts = new int[]{5555, 37012, 38888, 40000, 42000, 45555, 5554};

            for (String ip : targetIps) {
                for (int port : commonPorts) {
                    if (isPortOpen(ip, port, 300)) {
                        final String foundIp = ip;
                        final int foundPort = port;
                        mainHandler.post(() -> callback.onDeviceDiscovered(foundIp, foundPort));
                        return;
                    }
                }
            }

            mainHandler.post(() -> callback.onDeviceDiscovered(null, 0));
        }).start();
    }

    private static boolean isPortOpen(String ip, int port, int timeoutMs) {
        try {
            Socket socket = new Socket();
            socket.connect(new java.net.InetSocketAddress(ip, port), timeoutMs);
            socket.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public interface DiscoveryCallback {
        void onDeviceDiscovered(String ip, int port);
    }
}
