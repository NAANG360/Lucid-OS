package com.lucid.os.updater;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Downloads a newer updater APK, verifies the manifest checksum and package identity,
 * then stages it as a Magisk system_ext overlay. No recovery ZIP is involved.
 * A normal Android reboot is required so PackageManager can rescan the system app.
 */
public final class SelfUpdater {
    private static final String MANIFEST =
        "https://github.com/NAANG360/Lucid-OS/releases/download/lucid-system-updater-root-0.3-dev/LucidSystemUpdater-update.json";
    private static final String PACKAGE = "com.lucid.os.updater";
    private static final String MODULE = "/data/adb/modules/lucid-system-updater";
    private static final int MAX_MANIFEST_BYTES = 128 * 1024;
    private static final int MAX_APK_BYTES = 80 * 1024 * 1024;

    private SelfUpdater() {}

    public static void start(Activity activity, TextView status, Button button, ProgressBar progress) {
        button.setEnabled(false);
        progress.setIndeterminate(true);
        status.setText("Checking for a newer updater…");
        ExecutorService worker = Executors.newSingleThreadExecutor();
        Handler main = new Handler(Looper.getMainLooper());
        worker.execute(() -> {
            File apk = new File(activity.getFilesDir(), "lucid-updater-next.apk.part");
            try {
                JSONObject meta = getJson(MANIFEST);
                if (!PACKAGE.equals(meta.optString("packageName"))) {
                    throw new Exception("Updater manifest package mismatch.");
                }
                String url = meta.getString("url");
                String expected = meta.getString("sha256").toLowerCase(Locale.ROOT);
                if (!url.startsWith("https://") || !expected.matches("[0-9a-f]{64}") ||
                    expected.matches("0{64}")) {
                    throw new Exception("Updater release metadata is invalid.");
                }
                long targetCode = meta.getLong("versionCode");
                PackageInfo installed = activity.getPackageManager().getPackageInfo(PACKAGE, 0);
                long currentCode = Build.VERSION.SDK_INT >= 28
                    ? installed.getLongVersionCode() : installed.versionCode;
                if (targetCode <= currentCode) {
                    post(main, () -> status.setText("You're already on the latest published updater (" +
                        installed.versionName + ")."));
                    return;
                }
                post(main, () -> status.setText("Downloading updater " +
                    meta.optString("versionName", "new version") + "…"));
                downloadVerified(url, expected, apk, main, status, MAX_APK_BYTES);

                PackageManager pm = activity.getPackageManager();
                PackageInfo candidate = pm.getPackageArchiveInfo(apk.getAbsolutePath(), 0);
                if (candidate == null || candidate.applicationInfo == null ||
                    !PACKAGE.equals(candidate.packageName)) {
                    throw new Exception("Downloaded APK is not Lucid System Updater.");
                }
                long apkCode = Build.VERSION.SDK_INT >= 28
                    ? candidate.getLongVersionCode() : candidate.versionCode;
                if (apkCode != targetCode) throw new Exception("APK version does not match its manifest.");
                if (!sha256(apk).equals(expected)) throw new Exception("Checksum changed after download.");

                post(main, () -> status.setText("Checksum verified. Staging root system overlay…"));
                stageMagiskOverlay(apk);
                post(main, () -> {
                    progress.setIndeterminate(false);
                    progress.setProgress(100);
                    new AlertDialog.Builder(activity)
                        .setTitle("Updater ready")
                        .setMessage("Lucid System Updater " + meta.optString("versionName", "") +
                            " is staged through Magisk. Android must reboot normally to load the new system app; " +
                            "this does not reboot into recovery. Save your work before continuing.")
                        .setNegativeButton("Later", (dialog, which) -> {
                            status.setText("Updater staged. Reboot Android when convenient to finish the update.");
                            button.setEnabled(true);
                        })
                        .setPositiveButton("Reboot Android", (dialog, which) -> {
                            status.setText("Rebooting Android to load the new updater…");
                            new Thread(() -> {
                                try {
                                    root("reboot");
                                } catch (Exception e) {
                                    post(main, () -> {
                                        status.setText("Update staged, but reboot failed: " + e.getMessage());
                                        button.setEnabled(true);
                                    });
                                }
                            }, "LucidUpdaterReboot").start();
                        }).show();
                });
            } catch (Exception e) {
                String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                post(main, () -> {
                    progress.setIndeterminate(false);
                    status.setText("Updater self-update failed: " + msg);
                    button.setEnabled(true);
                });
            } finally {
                // Retain the APK only after staging succeeds; it is copied into Magisk's module.
                apk.delete();
                worker.shutdown();
            }
        });
    }

    private static JSONObject getJson(String address) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(address).openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(20000);
        c.setInstanceFollowRedirects(true);
        c.setRequestProperty("Accept", "application/json");
        try {
            int code = c.getResponseCode();
            if (code != 200) throw new Exception("Updater manifest HTTP " + code);
            try (InputStream in = c.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] b = new byte[4096];
                int n;
                while ((n = in.read(b)) != -1) {
                    if (out.size() + n > MAX_MANIFEST_BYTES) throw new Exception("Manifest too large.");
                    out.write(b, 0, n);
                }
                return new JSONObject(new String(out.toByteArray(), java.nio.charset.StandardCharsets.UTF_8));
            }
        } finally {
            c.disconnect();
        }
    }

    private static void downloadVerified(String address, String expected, File out,
                                         Handler main, TextView status, int limit) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(address).openConnection();
        c.setConnectTimeout(20000);
        c.setReadTimeout(60000);
        c.setInstanceFollowRedirects(true);
        c.setRequestProperty("Accept", "application/vnd.android.package-archive");
        try {
            int code = c.getResponseCode();
            if (code != 200) throw new Exception("APK download HTTP " + code);
            long length = c.getContentLengthLong();
            if (length > limit) throw new Exception("Updater APK exceeds the size limit.");
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            long total = 0;
            try (InputStream in = new BufferedInputStream(c.getInputStream());
                 FileOutputStream file = new FileOutputStream(out)) {
                byte[] b = new byte[65536];
                int n;
                while ((n = in.read(b)) != -1) {
                    total += n;
                    if (total > limit) throw new Exception("Updater APK exceeds the size limit.");
                    file.write(b, 0, n);
                    digest.update(b, 0, n);
                    if (length > 0) {
                        int pct = (int) Math.min(99, total * 100 / length);
                        post(main, () -> {
                            status.setText("Downloading updater… " + pct + "%");
                        });
                    }
                }
                file.getFD().sync();
            }
            StringBuilder actual = new StringBuilder();
            for (byte b : digest.digest()) actual.append(String.format(Locale.ROOT, "%02x", b & 255));
            if (!actual.toString().equalsIgnoreCase(expected)) throw new Exception("Updater APK SHA-256 mismatch.");
        } finally {
            c.disconnect();
        }
    }

    private static void stageMagiskOverlay(File apk) throws Exception {
        String apkPath = shellQuote(apk.getAbsolutePath());
        String moduleProp = "id=lucid-system-updater\nname=Lucid System Updater\nversion=managed\nversionCode=1\nauthor=Lucid OS\ndescription=Root-managed updater APK overlay\n";
        String encoded = android.util.Base64.encodeToString(
            moduleProp.getBytes(java.nio.charset.StandardCharsets.UTF_8), android.util.Base64.NO_WRAP);
        String script = "set -e; " +
            "test -d /data/adb/modules || { echo 'Magisk modules directory not found; root/Magisk is required.'; exit 31; }; " +
            "mkdir -p '" + MODULE + "/system_ext/priv-app/LucidSystemUpdater'; " +
            "cp " + apkPath + " '" + MODULE + "/system_ext/priv-app/LucidSystemUpdater/LucidSystemUpdater.apk'; " +
            "chmod 0755 '" + MODULE + "' '" + MODULE + "/system_ext' '" + MODULE + "/system_ext/priv-app' '" + MODULE + "/system_ext/priv-app/LucidSystemUpdater'; " +
            "chmod 0644 '" + MODULE + "/system_ext/priv-app/LucidSystemUpdater/LucidSystemUpdater.apk'; " +
            "chown 0:0 '" + MODULE + "/system_ext/priv-app/LucidSystemUpdater/LucidSystemUpdater.apk'; " +
            "printf %s '" + encoded + "' | base64 -d > '" + MODULE + "/module.prop'; " +
            "chmod 0644 '" + MODULE + "/module.prop'; " +
            "sync; " +
            "test -s '" + MODULE + "/system_ext/priv-app/LucidSystemUpdater/LucidSystemUpdater.apk'";
        root(script);
    }

    private static String shellQuote(String value) {
        return "'" + value.replace("'", "'\\''") + "'";
    }

    private static void root(String command) throws Exception {
        Process p = new ProcessBuilder("su", "-c", command).redirectErrorStream(true).start();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (InputStream in = p.getInputStream()) {
            byte[] b = new byte[4096];
            int n;
            while ((n = in.read(b)) != -1) {
                if (out.size() < 32768) out.write(b, 0, n);
            }
        }
        if (!p.waitFor(45, TimeUnit.SECONDS)) {
            p.destroyForcibly();
            throw new Exception("Root command timed out.");
        }
        if (p.exitValue() != 0) {
            throw new Exception("su exit " + p.exitValue() + ": " + out.toString("UTF-8"));
        }
    }

    private static String sha256(File file) throws Exception {
        MessageDigest d = MessageDigest.getInstance("SHA-256");
        try (InputStream in = new FileInputStream(file)) {
            byte[] b = new byte[65536];
            int n;
            while ((n = in.read(b)) != -1) d.update(b, 0, n);
        }
        StringBuilder s = new StringBuilder();
        for (byte b : d.digest()) s.append(String.format(Locale.ROOT, "%02x", b & 255));
        return s.toString();
    }

    private static void post(Handler handler, Runnable r) {
        handler.post(r);
    }
}
