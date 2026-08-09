package com.adb.scrcpy.connect.wifi;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Handler;
import android.os.Looper;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class AutoPairingEngine {
    private static final String SERVICE_TYPE_ADB_PAIRING = "_adb-tls-pairing._tcp.";
    private static final String SERVICE_TYPE_ADB_CONNECT = "_adb._tcp.";

    private final Context context;
    private final NsdManager nsdManager;
    private NsdManager.DiscoveryListener discoveryListener;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isScanning = false;

    public interface AutoPairCallback {
        void onDeviceDiscovered(String ip, int port, String serviceName);
    }

    public AutoPairingEngine(Context context) {
        this.context = context;
        this.nsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
    }

    public void startZeroTouchAutoPair(final AutoPairCallback callback) {
        if (isScanning || nsdManager == null) return;
        isScanning = true;

        discoveryListener = new NsdManager.DiscoveryListener() {
            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                isScanning = false;
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                isScanning = false;
            }

            @Override
            public void onDiscoveryStarted(String serviceType) {}

            @Override
            public void onDiscoveryStopped(String serviceType) {
                isScanning = false;
            }

            @Override
            public void onServiceFound(NsdServiceInfo serviceInfo) {
                nsdManager.resolveService(serviceInfo, new NsdManager.ResolveListener() {
                    @Override
                    public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {}

                    @Override
                    public void onServiceResolved(NsdServiceInfo resolvedService) {
                        InetAddress host = resolvedService.getHost();
                        final String ip = host != null ? host.getHostAddress() : "";
                        final int port = resolvedService.getPort();
                        final String name = resolvedService.getServiceName();

                        if (!ip.isEmpty() && port > 0) {
                            mainHandler.post(() -> callback.onDeviceDiscovered(ip, port, name));
                        }
                    }
                });
            }

            @Override
            public void onServiceLost(NsdServiceInfo serviceInfo) {}
        };

        try {
            nsdManager.discoverServices(SERVICE_TYPE_ADB_PAIRING, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
            nsdManager.discoverServices(SERVICE_TYPE_ADB_CONNECT, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
        } catch (Exception e) {
            isScanning = false;
        }
    }

    public void stopScan() {
        if (isScanning && nsdManager != null && discoveryListener != null) {
            try {
                nsdManager.stopServiceDiscovery(discoveryListener);
            } catch (Exception ignored) {}
            isScanning = false;
        }
    }
}
