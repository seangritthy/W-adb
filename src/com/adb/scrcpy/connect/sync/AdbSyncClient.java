package com.adb.scrcpy.connect.sync;

import com.adb.scrcpy.connect.adb.AdbConnection;
import com.adb.scrcpy.connect.adb.AdbStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class AdbSyncClient {

    public static class FileItem {
        public String name;
        public long size;
        public boolean isDirectory;
        public long lastModified;

        public FileItem(String name, long size, boolean isDirectory, long lastModified) {
            this.name = name;
            this.size = size;
            this.isDirectory = isDirectory;
            this.lastModified = lastModified;
        }
    }

    public static List<FileItem> listFiles(AdbConnection connection, String remotePath) throws Exception {
        List<FileItem> items = new ArrayList<>();
        AdbStream stream = connection.openStream("sync:");
        InputStream in = stream.getInputStream();

        byte[] req = createSyncHeader("LIST", remotePath);
        stream.write(req);

        byte[] headerBuf = new byte[8];
        while (true) {
            readFully(in, headerBuf, 0, 8);
            String id = new String(headerBuf, 0, 4, "UTF-8");
            ByteBuffer bb = ByteBuffer.wrap(headerBuf, 4, 4).order(ByteOrder.LITTLE_ENDIAN);
            int len = bb.getInt();

            if ("DONE".equals(id)) {
                break;
            } else if ("DENT".equals(id)) {
                byte[] dentBuf = new byte[16 + len];
                readFully(in, dentBuf, 0, dentBuf.length);
                ByteBuffer dentBb = ByteBuffer.wrap(dentBuf).order(ByteOrder.LITTLE_ENDIAN);
                int mode = dentBb.getInt();
                int size = dentBb.getInt();
                int mtime = dentBb.getInt();
                int nameLen = dentBb.getInt();

                String name = new String(dentBuf, 16, nameLen, "UTF-8");
                if (!".".equals(name) && !"..".equals(name)) {
                    boolean isDir = (mode & 0040000) != 0;
                    items.add(new FileItem(name, size, isDir, ((long) mtime) * 1000));
                }
            } else {
                break;
            }
        }
        stream.close();
        return items;
    }

    public static void pushFile(AdbConnection connection, File localFile, String remotePath, ProgressListener listener) throws Exception {
        AdbStream stream = connection.openStream("sync:");
        InputStream in = stream.getInputStream();

        String sendReqStr = remotePath + ",33206"; // S_IFREG | 0666
        byte[] req = createSyncHeader("SEND", sendReqStr);
        stream.write(req);

        FileInputStream fis = new FileInputStream(localFile);
        byte[] buffer = new byte[64 * 1024];
        long totalRead = 0;
        long fileSize = localFile.length();
        int read;

        while ((read = fis.read(buffer)) > 0) {
            byte[] chunkHeader = new byte[8];
            System.arraycopy("DATA".getBytes("UTF-8"), 0, chunkHeader, 0, 4);
            ByteBuffer.wrap(chunkHeader, 4, 4).order(ByteOrder.LITTLE_ENDIAN).putInt(read);
            
            stream.write(chunkHeader);
            if (read == buffer.length) {
                stream.write(buffer);
            } else {
                byte[] exact = new byte[read];
                System.arraycopy(buffer, 0, exact, 0, read);
                stream.write(exact);
            }

            totalRead += read;
            if (listener != null) {
                listener.onProgress(totalRead, fileSize);
            }
        }
        fis.close();

        // Send DONE header with mtime
        byte[] doneHeader = new byte[8];
        System.arraycopy("DONE".getBytes("UTF-8"), 0, doneHeader, 0, 4);
        ByteBuffer.wrap(doneHeader, 4, 4).order(ByteOrder.LITTLE_ENDIAN).putInt((int) (System.currentTimeMillis() / 1000));
        stream.write(doneHeader);

        // Read OKAY response
        byte[] respBuf = new byte[8];
        readFully(in, respBuf, 0, 8);
        String respId = new String(respBuf, 0, 4, "UTF-8");
        if (!"OKAY".equals(respId)) {
            throw new Exception("File push failed, server responded with: " + respId);
        }

        stream.close();
    }

    public static void pullFile(AdbConnection connection, String remotePath, File localFile, ProgressListener listener) throws Exception {
        AdbStream stream = connection.openStream("sync:");
        InputStream in = stream.getInputStream();

        byte[] req = createSyncHeader("RECV", remotePath);
        stream.write(req);

        FileOutputStream fos = new FileOutputStream(localFile);
        byte[] headerBuf = new byte[8];
        long totalReceived = 0;

        while (true) {
            readFully(in, headerBuf, 0, 8);
            String id = new String(headerBuf, 0, 4, "UTF-8");
            ByteBuffer bb = ByteBuffer.wrap(headerBuf, 4, 4).order(ByteOrder.LITTLE_ENDIAN);
            int len = bb.getInt();

            if ("DONE".equals(id)) {
                break;
            } else if ("DATA".equals(id)) {
                byte[] chunk = new byte[len];
                readFully(in, chunk, 0, len);
                fos.write(chunk);
                totalReceived += len;
                if (listener != null) {
                    listener.onProgress(totalReceived, -1);
                }
            } else {
                throw new Exception("Unexpected SYNC packet: " + id);
            }
        }
        fos.close();
        stream.close();
    }

    private static byte[] createSyncHeader(String cmd, String path) throws Exception {
        byte[] pathBytes = path.getBytes("UTF-8");
        byte[] header = new byte[8 + pathBytes.length];
        System.arraycopy(cmd.getBytes("UTF-8"), 0, header, 0, 4);
        ByteBuffer.wrap(header, 4, 4).order(ByteOrder.LITTLE_ENDIAN).putInt(pathBytes.length);
        System.arraycopy(pathBytes, 0, header, 8, pathBytes.length);
        return header;
    }

    private static void readFully(InputStream in, byte[] buf, int off, int len) throws Exception {
        int read = 0;
        while (read < len) {
            int n = in.read(buf, off + read, len - read);
            if (n < 0) throw new Exception("EOF reading SYNC stream");
            read += n;
        }
    }

    public interface ProgressListener {
        void onProgress(long transferred, long total);
    }
}
