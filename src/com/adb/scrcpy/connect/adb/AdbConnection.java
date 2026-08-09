package com.adb.scrcpy.connect.adb;

import com.adb.scrcpy.connect.crypto.AdbCrypto;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class AdbConnection {
    private Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private AdbCrypto crypto;
    private final AtomicInteger nextStreamId = new AtomicInteger(1);
    private final Map<Integer, AdbStream> streams = new ConcurrentHashMap<>();
    private boolean connected = false;
    private Thread readThread;

    public static AdbConnection createTcp(String host, int port, AdbCrypto crypto) throws Exception {
        Socket socket = new Socket();
        socket.setTcpNoDelay(true);
        socket.connect(new InetSocketAddress(host, port), 5000);
        AdbConnection conn = new AdbConnection();
        conn.socket = socket;
        conn.inputStream = socket.getInputStream();
        conn.outputStream = socket.getOutputStream();
        conn.crypto = crypto;
        return conn;
    }

    public void connect() throws Exception {
        byte[] payload = "host::W-adb\0".getBytes("UTF-8");
        sendMessage(AdbProtocol.Message.create(AdbProtocol.CMD_CNXN, 0x01000000, 4096, payload));

        AdbProtocol.Message response = AdbProtocol.Message.read(inputStream);

        if (response.command == AdbProtocol.CMD_AUTH) {
            if (response.arg0 == AdbProtocol.AUTH_TYPE_TOKEN) {
                byte[] token = response.payload;
                
                // 1. Try signing token with RSA private key
                byte[] signature = crypto.signAdbToken(token);
                sendMessage(AdbProtocol.Message.create(AdbProtocol.CMD_AUTH, AdbProtocol.AUTH_TYPE_SIGNATURE, 0, signature));
                response = AdbProtocol.Message.read(inputStream);

                // 2. If signature fails (device hasn't accepted public key yet), send RSA public key
                if (response.command == AdbProtocol.CMD_AUTH) {
                    byte[] pubKey = crypto.getAdbPublicKeyPayload();
                    sendMessage(AdbProtocol.Message.create(AdbProtocol.CMD_AUTH, AdbProtocol.AUTH_TYPE_RSAKEY, 0, pubKey));
                    
                    // Device will show "Allow USB/Wireless Debugging?" dialog on screen
                    response = AdbProtocol.Message.read(inputStream);
                }
            }
        }

        if (response.command != AdbProtocol.CMD_CNXN) {
            throw new Exception("ADB Auth Pending/Refused. Please check Target Device Screen and tap 'Allow Wireless Debugging'!");
        }

        connected = true;
        startReadLoop();
    }

    private synchronized void sendMessage(AdbProtocol.Message msg) throws Exception {
        outputStream.write(msg.getHeaderBytes());
        if (msg.payload != null && msg.payload.length > 0) {
            outputStream.write(msg.payload);
        }
        outputStream.flush();
    }

    private void startReadLoop() {
        readThread = new Thread(() -> {
            try {
                while (connected) {
                    AdbProtocol.Message msg = AdbProtocol.Message.read(inputStream);
                    switch (msg.command) {
                        case AdbProtocol.CMD_OKAY: {
                            int localId = msg.arg1;
                            int remoteId = msg.arg0;
                            AdbStream stream = streams.get(localId);
                            if (stream != null) {
                                stream.setRemoteId(remoteId);
                            }
                            break;
                        }
                        case AdbProtocol.CMD_WRTE: {
                            int localId = msg.arg1;
                            int remoteId = msg.arg0;
                            AdbStream stream = streams.get(localId);
                            if (stream != null) {
                                stream.receive(msg.payload);
                                sendMessage(AdbProtocol.Message.create(AdbProtocol.CMD_OKAY, localId, remoteId, null));
                            }
                            break;
                        }
                        case AdbProtocol.CMD_CLSE: {
                            int localId = msg.arg1;
                            AdbStream stream = streams.remove(localId);
                            if (stream != null) {
                                stream.close();
                            }
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                connected = false;
            }
        });
        readThread.setDaemon(true);
        readThread.start();
    }

    public AdbStream openStream(String destination) throws Exception {
        int localId = nextStreamId.getAndIncrement();
        AdbStream stream = new AdbStream(this, localId);
        streams.put(localId, stream);

        byte[] destBytes = (destination + "\0").getBytes("UTF-8");
        sendMessage(AdbProtocol.Message.create(AdbProtocol.CMD_OPEN, localId, 0, destBytes));

        // Wait until OKAY is received
        long start = System.currentTimeMillis();
        while (stream.getRemoteId() == 0 && System.currentTimeMillis() - start < 10000) {
            Thread.sleep(20);
        }

        if (stream.getRemoteId() == 0) {
            streams.remove(localId);
            throw new Exception("Timeout waiting for stream open response for: " + destination);
        }

        return stream;
    }

    public void writeStream(int localId, int remoteId, byte[] data) throws Exception {
        sendMessage(AdbProtocol.Message.create(AdbProtocol.CMD_WRTE, localId, remoteId, data));
    }

    public void closeStream(int localId, int remoteId) throws Exception {
        sendMessage(AdbProtocol.Message.create(AdbProtocol.CMD_CLSE, localId, remoteId, null));
    }

    public boolean isConnected() {
        return connected;
    }

    public void close() {
        connected = false;
        try {
            if (socket != null) socket.close();
        } catch (Exception ignored) {}
    }
}
