# AdbScrcpyConnect v1.0.0 Release Notes

## Features Included
- **Wireless ADB & USB OTG**: Auto-detects local Wi-Fi / Hotspot IP addresses and connects via TCP/IP or Android UsbManager bulk endpoints.
- **Scrcpy v2.4 Remote Control**: Pushes embedded `scrcpy-server-v2.4.jar`, decodes H.264 video with low latency via `MediaCodec`, and injects real-time touch & navigation key events (Home, Back, Recents).
- **ADB SYNC High-Speed File Manager**: Browse remote `/sdcard/` directories, upload files, download files, and manage storage between Android devices.
- **User 1 & User 2 Workflow UI**: Built-in guidance for User 2 (Target Phone) and User 1 (Scrcpy Viewer Phone).

## Build Verification
- Signed with Android Debug RSA key pair (v1, v2, v3 schemes).
- Tested and verified on Termux JVM runtime environment.
