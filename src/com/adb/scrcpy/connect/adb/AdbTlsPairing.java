package com.adb.scrcpy.connect.adb;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class AdbTlsPairing {

    public static boolean pairDevice(String host, int pairPort, String pairCode) throws Exception {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        TrustManager[] trustAll = new TrustManager[]{
            new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                public void checkServerTrusted(X509Certificate[] certs, String authType) {}
            }
        };

        sslContext.init(null, trustAll, new SecureRandom());
        SSLSocketFactory factory = sslContext.getSocketFactory();

        SSLSocket socket = (SSLSocket) factory.createSocket();
        socket.connect(new InetSocketAddress(host, pairPort), 5000);
        socket.startHandshake();

        OutputStream out = socket.getOutputStream();
        InputStream in = socket.getInputStream();

        // ADB TLS Pairing Payload: send 6-digit pairing code header
        byte[] codeBytes = pairCode.getBytes("UTF-8");
        byte[] payload = new byte[4 + codeBytes.length];
        payload[0] = (byte) codeBytes.length;
        System.arraycopy(codeBytes, 0, payload, 4, codeBytes.length);

        out.write(payload);
        out.flush();

        byte[] resp = new byte[16];
        int read = in.read(resp);
        socket.close();

        return read > 0;
    }
}
