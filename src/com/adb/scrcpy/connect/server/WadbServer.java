package com.adb.scrcpy.connect.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;

public class WadbServer {
    public static final int SERVER_PORT = 9999;
    private static boolean isRunning = true;

    public static void main(String[] args) {
        System.out.println("W-adb Shizuku-Style Server Started. UID: " + android.os.Process.myUid());
        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            while (isRunning) {
                Socket client = serverSocket.accept();
                new Thread(() -> handleClient(client)).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket client) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
             PrintWriter out = new PrintWriter(client.getOutputStream(), true)) {

            String command = in.readLine();
            if ("PING".equals(command)) {
                out.println("PONG UID=" + android.os.Process.myUid());
            } else if (command != null && command.startsWith("EXEC:")) {
                String shellCmd = command.substring(5);
                Process p = Runtime.getRuntime().exec(shellCmd);
                BufferedReader cmdIn = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line;
                while ((line = cmdIn.readLine()) != null) {
                    out.println(line);
                }
                p.waitFor();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Object getSystemService(String name) {
        try {
            Class<?> sm = Class.forName("android.os.ServiceManager");
            Method getService = sm.getMethod("getService", String.class);
            return getService.invoke(null, name);
        } catch (Exception e) {
            return null;
        }
    }
}
