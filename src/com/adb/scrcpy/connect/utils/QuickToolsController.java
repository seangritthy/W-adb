package com.adb.scrcpy.connect.utils;

import com.adb.scrcpy.connect.adb.AdbConnection;
import com.adb.scrcpy.connect.adb.AdbStream;
import com.adb.scrcpy.connect.sync.AdbSyncClient;

import java.io.File;
import java.io.InputStream;

public class QuickToolsController {

    public static void takeScreenshot(AdbConnection connection, File saveFile) throws Exception {
        AdbStream stream = connection.openStream("shell:screencap -p");
        InputStream in = stream.getInputStream();

        java.io.FileOutputStream fos = new java.io.FileOutputStream(saveFile);
        byte[] buf = new byte[8192];
        int read;
        while ((read = in.read(buf)) > 0) {
            fos.write(buf, 0, read);
        }
        fos.close();
        stream.close();
    }

    public static void turnOffScreen(AdbConnection connection) throws Exception {
        executeShell(connection, "input keyevent 26");
    }

    public static void pressHome(AdbConnection connection) throws Exception {
        executeShell(connection, "input keyevent 3");
    }

    public static void pressBack(AdbConnection connection) throws Exception {
        executeShell(connection, "input keyevent 4");
    }

    public static void setClipboardText(AdbConnection connection, String text) throws Exception {
        String safeText = text.replace("'", "\\'");
        executeShell(connection, "cmd clipboard set text '" + safeText + "'");
    }

    public static String getInstalledPackages(AdbConnection connection) throws Exception {
        return executeShell(connection, "pm list packages -3");
    }

    private static String executeShell(AdbConnection connection, String command) throws Exception {
        AdbStream stream = connection.openStream("shell:" + command);
        InputStream in = stream.getInputStream();
        StringBuilder sb = new StringBuilder();
        byte[] buf = new byte[4096];
        int read;
        while ((read = in.read(buf)) > 0) {
            sb.append(new String(buf, 0, read, "UTF-8"));
        }
        stream.close();
        return sb.toString();
    }
}
