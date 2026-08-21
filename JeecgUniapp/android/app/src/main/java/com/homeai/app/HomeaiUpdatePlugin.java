package com.homeai.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import androidx.core.content.FileProvider;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@CapacitorPlugin(name = "HomeaiUpdate")
public class HomeaiUpdatePlugin extends Plugin {

    private static final int MAX_ZIP_ENTRIES = 8000;
    private static final long MAX_UNCOMPRESSED = 200L * 1024 * 1024;

    @PluginMethod
    public void download(PluginCall call) {
        String url = call.getString("url");
        String fileName = call.getString("fileName", "update.bin");
        if (url == null || url.isEmpty()) {
            call.reject("缺少下载地址");
            return;
        }
        new Thread(() -> {
            try {
                File dest = downloadTo(url, sanitizeName(fileName));
                JSObject ret = new JSObject();
                ret.put("path", dest.getAbsolutePath());
                call.resolve(ret);
            } catch (Exception e) {
                call.reject("下载失败: " + e.getMessage());
            }
        }).start();
    }

    @PluginMethod
    public void sha256(PluginCall call) {
        String path = call.getString("path");
        if (path == null || path.isEmpty()) {
            call.reject("缺少文件路径");
            return;
        }
        new Thread(() -> {
            try {
                JSObject ret = new JSObject();
                ret.put("hash", sha256Hex(new File(path)));
                call.resolve(ret);
            } catch (Exception e) {
                call.reject("校验失败: " + e.getMessage());
            }
        }).start();
    }

    @PluginMethod
    public void unzip(PluginCall call) {
        String zipPath = call.getString("zipPath");
        String destDirName = call.getString("destDirName", "web");
        if (zipPath == null || zipPath.isEmpty()) {
            call.reject("缺少压缩包路径");
            return;
        }
        new Thread(() -> {
            try {
                File dest = unzipSafe(new File(zipPath), sanitizeName(destDirName));
                File index = new File(dest, "index.html");
                if (!index.isFile()) {
                    call.reject("热更新包根目录缺少 index.html");
                    return;
                }
                JSObject ret = new JSObject();
                ret.put("path", dest.getAbsolutePath());
                call.resolve(ret);
            } catch (Exception e) {
                call.reject("解压失败: " + e.getMessage());
            }
        }).start();
    }

    @PluginMethod
    public void installApk(PluginCall call) {
        String path = call.getString("path");
        if (path == null || path.isEmpty()) {
            call.reject("缺少 APK 路径");
            return;
        }
        Activity activity = getActivity();
        if (activity == null) {
            call.reject("无法安装");
            return;
        }
        activity.runOnUiThread(() -> {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        && !activity.getPackageManager().canRequestPackageInstalls()) {
                    Intent settings = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                    settings.setData(Uri.parse("package:" + activity.getPackageName()));
                    settings.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    activity.startActivity(settings);
                    call.reject("NEED_PERMISSION");
                    return;
                }
                File apk = new File(path);
                if (!apk.isFile()) {
                    call.reject("APK 不存在");
                    return;
                }
                Uri uri = FileProvider.getUriForFile(
                        activity, activity.getPackageName() + ".fileprovider", apk);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(uri, "application/vnd.android.package-archive");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivity(intent);
                call.resolve();
            } catch (Exception e) {
                call.reject("安装失败: " + e.getMessage());
            }
        });
    }

    private File downloadTo(String url, String fileName) throws Exception {
        File dir = new File(getContext().getCacheDir(), "homeai-update");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new Exception("无法创建下载目录");
        }
        File dest = new File(dir, fileName);
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(20000);
        conn.setReadTimeout(120000);
        conn.setInstanceFollowRedirects(true);
        conn.connect();
        int code = conn.getResponseCode();
        if (code >= 400) {
            throw new Exception("HTTP " + code);
        }
        try (InputStream in = conn.getInputStream(); FileOutputStream out = new FileOutputStream(dest)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
        } finally {
            conn.disconnect();
        }
        return dest;
    }

    private File unzipSafe(File zipFile, String destDirName) throws Exception {
        File destRoot = new File(getContext().getFilesDir(), "homeai-web/" + destDirName);
        deleteRecursively(destRoot);
        if (!destRoot.mkdirs()) {
            throw new Exception("无法创建解压目录");
        }
        String destAbs = destRoot.getCanonicalPath();
        int entries = 0;
        long uncompressed = 0;
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            byte[] buf = new byte[8192];
            while ((entry = zis.getNextEntry()) != null) {
                entries++;
                if (entries > MAX_ZIP_ENTRIES) {
                    throw new Exception("压缩包内文件过多");
                }
                String name = entry.getName().replace('\\', '/');
                if (name.startsWith("/") || name.contains("..") || name.startsWith("__MACOSX") || name.endsWith(".DS_Store")) {
                    continue;
                }
                File outFile = new File(destRoot, name);
                String outAbs = outFile.getCanonicalPath();
                if (!outAbs.startsWith(destAbs + File.separator) && !outAbs.equals(destAbs)) {
                    throw new Exception("压缩包路径不合法");
                }
                if (entry.isDirectory()) {
                    if (!outFile.exists() && !outFile.mkdirs()) {
                        throw new Exception("无法创建目录");
                    }
                    continue;
                }
                File parent = outFile.getParentFile();
                if (parent != null && !parent.exists() && !parent.mkdirs()) {
                    throw new Exception("无法创建目录");
                }
                try (FileOutputStream out = new FileOutputStream(outFile)) {
                    int n;
                    while ((n = zis.read(buf)) > 0) {
                        uncompressed += n;
                        if (uncompressed > MAX_UNCOMPRESSED) {
                            throw new Exception("解压后过大");
                        }
                        out.write(buf, 0, n);
                    }
                }
            }
        }
        return destRoot;
    }

    private static String sha256Hex(File file) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (InputStream in = new FileInputStream(file)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                md.update(buf, 0, n);
            }
        }
        byte[] digest = md.digest();
        StringBuilder sb = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static void deleteRecursively(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteRecursively(child);
            }
        }
        //noinspection ResultOfMethodCallIgnored
        file.delete();
    }

    private static String sanitizeName(String name) {
        if (name == null || name.isEmpty()) {
            return "update.bin";
        }
        return name.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
