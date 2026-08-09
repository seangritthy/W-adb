package com.adb.scrcpy.connect.adb;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class AdbProtocol {
    public static final int CMD_SYNC = 0x434e5953;
    public static final int CMD_CNXN = 0x4e584e43;
    public static final int CMD_AUTH = 0x48545541;
    public static final int CMD_OPEN = 0x4e45504f;
    public static final int CMD_OKAY = 0x59414b4f;
    public static final int CMD_CLSE = 0x45534c43;
    public static final int CMD_WRTE = 0x45545257;

    public static final int AUTH_TYPE_TOKEN = 1;
    public static final int AUTH_TYPE_SIGNATURE = 2;
    public static final int AUTH_TYPE_RSAKEY = 3;

    public static final int MAX_PAYLOAD = 4096;

    public static class Message {
        public int command;
        public int arg0;
        public int arg1;
        public int dataLength;
        public int dataChecksum;
        public int magic;
        public byte[] payload;

        public static Message create(int command, int arg0, int arg1, byte[] payload) {
            Message msg = new Message();
            msg.command = command;
            msg.arg0 = arg0;
            msg.arg1 = arg1;
            msg.payload = payload != null ? payload : new byte[0];
            msg.dataLength = msg.payload.length;
            msg.dataChecksum = checksum(msg.payload);
            msg.magic = command ^ 0xFFFFFFFF;
            return msg;
        }

        public byte[] getHeaderBytes() {
            ByteBuffer buf = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN);
            buf.putInt(command);
            buf.putInt(arg0);
            buf.putInt(arg1);
            buf.putInt(dataLength);
            buf.putInt(dataChecksum);
            buf.putInt(magic);
            return buf.array();
        }

        public static Message read(InputStream in) throws Exception {
            byte[] header = new byte[24];
            int read = 0;
            while (read < 24) {
                int n = in.read(header, read, 24 - read);
                if (n < 0) throw new Exception("EOF reading ADB header");
                read += n;
            }

            ByteBuffer buf = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
            Message msg = new Message();
            msg.command = buf.getInt();
            msg.arg0 = buf.getInt();
            msg.arg1 = buf.getInt();
            msg.dataLength = buf.getInt();
            msg.dataChecksum = buf.getInt();
            msg.magic = buf.getInt();

            if (msg.dataLength > 0) {
                msg.payload = new byte[msg.dataLength];
                int pRead = 0;
                while (pRead < msg.dataLength) {
                    int n = in.read(msg.payload, pRead, msg.dataLength - pRead);
                    if (n < 0) throw new Exception("EOF reading ADB payload");
                    pRead += n;
                }
            } else {
                msg.payload = new byte[0];
            }
            return msg;
        }
    }

    public static int checksum(byte[] data) {
        if (data == null) return 0;
        int res = 0;
        for (byte b : data) {
            res += (b & 0xFF);
        }
        return res;
    }
}
