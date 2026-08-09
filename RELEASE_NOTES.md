# W-adb Release Notes & Release History

## Latest Release: v1.0.1
- **Auto-Updater & System Package Installer**: Added `AppUpdater` module that checks GitHub releases, downloads update `.apk` assets, and triggers Android System Package Installer (`REQUEST_INSTALL_PACKAGES` permission).
- **Custom ContentProvider (`AppFileProvider`)**: Clean Android ContentProvider implementation for sharing download APK URIs securely with Android Package Installer.
- **Version Bump**: `versionCode 2`, `versionName 1.0.1`.

---

## v1.0.0
- **Wireless ADB & Scrcpy v2.4 Remote Display**: Instant Wi-Fi pair & connect for User 1 (Viewer) and User 2 (Target Phone).
- **Interactive Touch & Navigation Controls**: Hardware H.264 video decoding with Android `MediaCodec` + touch event injection.
- **ADB SYNC File Manager**: Remote directory listing, file upload, and file download over ADB channels.
