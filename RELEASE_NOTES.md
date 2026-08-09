# W-adb Release Notes & Release History

## Latest Release: v1.5.0
- **Consistent Debug Keystore Signing**: Fixed persistent APK signing key so Android package manager seamlessly updates installed 1.0.1 APKs without signature mismatch errors.
- **UI Version Header Badge**: Added live version badge `v1.5.0 (10500)` inside the app header bar.
- **Version Bump**: `versionCode 10500`, `versionName 1.5.0`.

---

## v1.4.1
- **Removed Hardcoded Fallback Version**: Fixed `AppUpdater.java` to dynamically check runtime `PackageManager` version info.

---

## v1.4.0
- **Direct Wi-Fi Screen Share Mode (NO ADB / NO Wireless Debugging Needed!)**: Added `ScreenSenderServer` (`MediaProjection`) and `ScreenReceiverClient` to view live screens over local Wi-Fi / Mobile Hotspot without Wireless Debugging or a PC!
