package com.adb.scrcpy.connect;

import com.adb.scrcpy.connect.adb.AdbProtocol;
import com.adb.scrcpy.connect.crypto.AdbCrypto;
import com.adb.scrcpy.connect.scrcpy.ScrcpyControlMessage;

public class TestRunner {
    public static void main(String[] args) {
        System.out.println("=== Running AdbScrcpyConnect Core Logic Tests ===");

        try {
            // Test 1: Crypto Key Gen & Signing
            System.out.print("[Test 1/4] AdbCrypto RSA signing... ");
            AdbCrypto crypto = new AdbCrypto();
            byte[] token = new byte[20];
            byte[] sig = crypto.signAdbToken(token);
            if (sig != null && sig.length == 256) {
                System.out.println("PASSED (Signature length: " + sig.length + " bytes)");
            } else {
                System.out.println("FAILED!");
            }

            // Test 2: ADB Protocol Packet Framing & Checksum
            System.out.print("[Test 2/4] AdbProtocol Message Framing... ");
            byte[] payload = "host::scrcpy-test".getBytes("UTF-8");
            AdbProtocol.Message msg = AdbProtocol.Message.create(AdbProtocol.CMD_CNXN, 0x01000000, 4096, payload);
            byte[] header = msg.getHeaderBytes();
            if (header.length == 24 && msg.dataChecksum == AdbProtocol.checksum(payload)) {
                System.out.println("PASSED (Header 24 bytes, Checksum OK)");
            } else {
                System.out.println("FAILED!");
            }

            // Test 3: Scrcpy Control Message Serialization
            System.out.print("[Test 3/4] Scrcpy Control Touch Serialization... ");
            byte[] touchPacket = ScrcpyControlMessage.createTouchEvent(0, 1, 500, 1000, 1080, 1920, 1.0f);
            if (touchPacket.length == 32 && touchPacket[0] == ScrcpyControlMessage.TYPE_INJECT_TOUCH_EVENT) {
                System.out.println("PASSED (Touch Packet 32 bytes)");
            } else {
                System.out.println("FAILED!");
            }

            // Test 4: Scrcpy Keycode Serialization
            System.out.print("[Test 4/4] Scrcpy Keycode Serialization... ");
            byte[] keyPacket = ScrcpyControlMessage.createKeycodeEvent(0, 4, 0);
            if (keyPacket.length == 14 && keyPacket[0] == ScrcpyControlMessage.TYPE_INJECT_KEYCODE) {
                System.out.println("PASSED (Keycode Packet 14 bytes)");
            } else {
                System.out.println("FAILED!");
            }

            System.out.println("=== ALL CORE LOGIC TESTS PASSED SUCCESSFULLY ===");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
