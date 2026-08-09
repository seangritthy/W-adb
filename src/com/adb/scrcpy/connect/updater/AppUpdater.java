package com.adb.scrcpy.connect.updater;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class AppUpdater {
    private static final String GITHUB_RELEASE_URL = "https://api.github.com/repos/seangritthy/W-adb/releases/latest";
    private static final String DEFAULT_FALLBACK_APK_URL = "https://raw.githubusercontent.com/seangritthy/vdomov-apks/main/W-adb.apk";

    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public AppUpdater(Context context) {
        this.context = context;
    }

    public String getInstalledVersionName() {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return pInfo.versionName;
        } catch (Exception e) {
            return "1.4.0";
        }
    }

    public void checkForUpdates(final UpdateCallback callback) {
        new Thread(() -> {
            try {
                URL url = new URL(GITHUB_RELEASE_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("User-Agent", "W-adb-Android-App");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                if (conn.getResponseCode() == 200) {
                    Scanner scanner = new Scanner(conn.getInputStream()).useDelimiter("\\A");
                    String response = scanner.hasNext() ? scanner.next() : "";
                    
                    String latestVersion = extractJsonValue(response, "tag_name");
                    String downloadUrl = extractJsonValue(response, "browser_download_url");

                    if (downloadUrl.isEmpty()) {
                        downloadUrl = DEFAULT_FALLBACK_APK_URL;
                    }

                    String currentVersion = getInstalledVersionName();
                    boolean hasUpdate = latestVersion != null && !latestVersion.isEmpty() && !latestVersion.contains(currentVersion);

                    final String finalVersion = latestVersion;
                    final String finalDownloadUrl = downloadUrl;
                    final boolean finalHasUpdate = hasUpdate;

                    mainHandler.post(() -> callback.onUpdateCheckResult(finalHasUpdate, finalVersion, finalDownloadUrl));
                } else {
                    mainHandler.post(() -> callback.onUpdateCheckResult(false, null, null));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onUpdateCheckResult(false, null, null));
            }
        }).start();
    }

    public void downloadAndInstall(final String downloadUrl) {
        Toast.makeText(context, "Downloading update...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            try {
                URL url = new URL(downloadUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setInstanceFollowRedirects(true);
                InputStream in = conn.getInputStream();

                File apkFile = new File(context.getCacheDir(), "update.apk");
                FileOutputStream fos = new FileOutputStream(apkFile);

                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) > 0) {
                    fos.write(buffer, 0, read);
                }
                fos.close();
                in.close();

                mainHandler.post(() -> installApk(apkFile));
            } catch (Exception e) {
                mainHandler.post(() -> Toast.makeText(context, "Update Download Failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    public void installApk(File apkFile) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri contentUri = Uri.parse("content://" + context.getPackageName() + ".fileprovider/" + apkFile.getName());
            intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "Installer Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\":\"";
        int start = json.indexOf(searchKey);
        if (start < 0) return "";
        start += searchKey.length();
        int end = json.indexOf("\"", start);
        if (end < 0) return "";
        return json.substring(start, end);
    }

    public interface UpdateCallback {
        void onUpdateCheckResult(boolean hasUpdate, String latestVersion, String downloadUrl);
    }
}
