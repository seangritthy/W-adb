# W-adb Release Notes & Release History

## Latest Release: v1.3.1
- **Version Alignment**: Updated `versionName` to `1.3.1` and bumped `versionCode` to `10301` to align Android App Info settings with GitHub Release versioning.
- **System Settings Compatibility**: Guaranteed that Android System -> App Info displays version `1.3.1`.

---

## v1.2.0
- **Built-in Mobile Hotspot Auto-Pairing (`HotspotPairingManager`)**: Added **AUTO-PAIR HOTSPOT DEVICE** scanner that automatically detects connected Mobile Hotspot clients/gateway IPs and open Wireless ADB ports without needing manual IP typing!
- **Wi-Fi Subnet Scanner**: Auto-scans `/proc/net/arp` and Wi-Fi gateway interfaces for zero-configuration device pairing.

---

## v1.1.1
- **ADB Auth Handshake & Socket Fix**: Added null-terminated system identity string (`host::W-adb\0`) per ADB protocol spec.
- **Screen Popup Prompting**: Improved authentication flow so target device displays "Allow Wireless Debugging?" RSA key prompt.

---

## v1.1.0
- **Local Loopback Mode (Phone-to-Self LADB Mode)**: Connect to `127.0.0.1` locally via Android Wireless Debugging directly on your phone without needing a PC or root!
- **Interactive ADB Shell Console**: Added ADB Shell tab (`💻 ADB Shell`) for running commands (`pm list packages`, `setprop`, `wm density`, `dumpsys`, etc.) directly on local or remote devices.

---

## v1.0.1
- **Auto-Updater & System Package Installer**: Added `AppUpdater` module that checks GitHub releases, downloads update `.apk` assets, and triggers Android System Package Installer.
