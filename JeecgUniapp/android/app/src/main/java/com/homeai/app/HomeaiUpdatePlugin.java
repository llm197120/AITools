package com.homeai.app;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.util.Base64;
import androidx.activity.result.ActivityResult;
import androidx.core.content.FileProvider;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.ActivityCallback;
import com.getcapacitor.annotation.CapacitorPlugin;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@CapacitorPlugin(name = "HomeaiUpdate")
public class HomeaiUpdatePlugin extends Plugin {

    private static final int MAX_ZIP_ENTRIES = 8000;
    private static final long MAX_UNCOMPRESSED = 200L * 1024 * 1024;
    private static final long MAX_PICK_BYTES = 500L * 1024 * 1024;

    @PluginMethod
    public void download(PluginCall call) {
        String url = call.getString("url");
        String fileName = call.getString("fileName", "update.bin");
        if (url == null || url.isEmpty()) {
            call.reject("缺少下载地址");
            return;
        }
        JSObject headers = call.getObject("headers");
        new Thread(() -> {
            try {
                File dest = downloadTo(url, sanitizeName(fileName), headers);
                JSObject ret = new JSObject();
                ret.put("path", dest.getAbsolutePath());
                call.resolve(ret);
            } catch (Exception e) {
                call.reject("下载失败: " + e.getMessage());
            }
        }).start();
    }

    @PluginMethod
    public void openFile(PluginCall call) {
        String path = call.getString("path");
        String mime = call.getString("mime", "application/octet-stream");
        if (path == null || path.isEmpty()) {
            call.reject("缺少文件路径");
            return;
        }
        Activity activity = getActivity();
        if (activity == null) {
            call.reject("无法打开文件");
            return;
        }
        String type = mime == null || mime.isEmpty() ? "application/octet-stream" : mime;
        activity.runOnUiThread(() -> {
            try {
                File file = new File(path);
                if (!file.isFile()) {
                    call.reject("文件不存在");
                    return;
                }
                Uri uri = FileProvider.getUriForFile(
                        activity, activity.getPackageName() + ".fileprovider", file);
                Intent view = new Intent(Intent.ACTION_VIEW);
                view.setDataAndType(uri, type);
                view.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                List<ResolveInfo> targets = activity.getPackageManager()
                        .queryIntentActivities(view, PackageManager.MATCH_DEFAULT_ONLY);
                for (ResolveInfo info : targets) {
                    activity.grantUriPermission(
                            info.activityInfo.packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
                Intent chooser = Intent.createChooser(view, "打开文件");
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivity(chooser);
                call.resolve();
            } catch (Exception e) {
                call.reject("打开失败: " + e.getMessage());
            }
        });
    }

    @PluginMethod
    public void readBase64(PluginCall call) {
        String path = call.getString("path");
        if (path == null || path.isEmpty()) {
            call.reject("缺少文件路径");
            return;
        }
        new Thread(() -> {
            try {
                File file = new File(path);
                if (!file.isFile()) {
                    call.reject("文件不存在");
                    return;
                }
                byte[] bytes = new byte[(int) file.length()];
                try (FileInputStream in = new FileInputStream(file)) {
                    int off = 0;
                    int n;
                    while (off < bytes.length && (n = in.read(bytes, off, bytes.length - off)) > 0) {
                        off += n;
                    }
                }
                JSObject ret = new JSObject();
                ret.put("data", Base64.encodeToString(bytes, Base64.NO_WRAP));
                call.resolve(ret);
            } catch (Exception e) {
                call.reject("读取失败: " + e.getMessage());
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

    /** 系统文档选择器（ACTION_OPEN_DOCUMENT），避免 WebView input 只弹出相册视频 */
    @PluginMethod
    public void pickFile(PluginCall call) {
        Activity activity = getActivity();
        if (activity == null) {
            call.reject("无法选择文件");
            return;
        }
        boolean multiple = Boolean.TRUE.equals(call.getBoolean("multiple", false));
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        List<String> mimes = readMimeTypes(call);
        if (mimes.isEmpty()) {
            String mime = call.getString("mime", "*/*");
            intent.setType(mime == null || mime.isEmpty() ? "*/*" : mime);
        } else if (mimes.size() == 1) {
            intent.setType(mimes.get(0));
        } else {
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimes.toArray(new String[0]));
        }
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, multiple);
        startActivityForResult(call, intent, "onPickFileResult");
    }

    @ActivityCallback
    private void onPickFileResult(PluginCall call, ActivityResult result) {
        if (call == null) {
            return;
        }
        if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null) {
            JSObject ret = new JSObject();
            ret.put("files", new JSArray());
            call.resolve(ret);
            return;
        }
        List<Uri> uris = collectUris(result.getData());
        new Thread(() -> {
            try {
                JSArray files = new JSArray();
                for (Uri uri : uris) {
                    files.put(copyUriToCache(uri));
                }
                JSObject ret = new JSObject();
                ret.put("files", files);
                call.resolve(ret);
            } catch (Exception e) {
                call.reject("选择失败: " + e.getMessage());
            }
        }).start();
    }

    private static List<String> readMimeTypes(PluginCall call) {
        List<String> mimes = new ArrayList<>();
        JSArray arr = call.getArray("mimeTypes");
        if (arr == null) {
            return mimes;
        }
        for (int i = 0; i < arr.length(); i++) {
            String mime = arr.optString(i, "");
            if (mime != null && !mime.isEmpty()) {
                mimes.add(mime);
            }
        }
        return mimes;
    }

    private static List<Uri> collectUris(Intent data) {
        List<Uri> uris = new ArrayList<>();
        if (data == null) {
            return uris;
        }
        ClipData clip = data.getClipData();
        if (clip != null) {
            for (int i = 0; i < clip.getItemCount(); i++) {
                Uri uri = clip.getItemAt(i).getUri();
                if (uri != null) {
                    uris.add(uri);
                }
            }
            return uris;
        }
        if (data.getData() != null) {
            uris.add(data.getData());
        }
        return uris;
    }

    private String queryDisplayName(Uri uri) {
        Cursor cursor = getContext().getContentResolver().query(
                uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (idx >= 0) {
                        String name = cursor.getString(idx);
                        if (name != null && !name.isEmpty()) {
                            return name;
                        }
                    }
                }
            } finally {
                cursor.close();
            }
        }
        return "file-" + System.currentTimeMillis();
    }

    private JSObject copyUriToCache(Uri uri) throws Exception {
        String displayName = queryDisplayName(uri);
        String mime = getContext().getContentResolver().getType(uri);
        if (mime == null || mime.isEmpty()) {
            mime = "application/octet-stream";
        }
        File dir = new File(getContext().getCacheDir(), "homeai-pick");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new Exception("无法创建临时目录");
        }
        File dest = new File(dir, System.currentTimeMillis() + "-" + sanitizeName(displayName));
        long copied = 0;
        try (InputStream in = getContext().getContentResolver().openInputStream(uri);
             FileOutputStream out = new FileOutputStream(dest)) {
            if (in == null) {
                throw new Exception("无法读取所选文件");
            }
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                copied += n;
                if (copied > MAX_PICK_BYTES) {
                    //noinspection ResultOfMethodCallIgnored
                    dest.delete();
                    throw new Exception("文件过大");
                }
                out.write(buf, 0, n);
            }
        }
        JSObject item = new JSObject();
        item.put("path", dest.getAbsolutePath());
        item.put("name", displayName);
        item.put("size", dest.length());
        item.put("mimeType", mime);
        return item;
    }

    private File downloadTo(String url, String fileName, JSObject headers) throws Exception {
        File dir = new File(getContext().getCacheDir(), "homeai-update");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new Exception("无法创建下载目录");
        }
        File dest = new File(dir, fileName);
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(20000);
        conn.setReadTimeout(120000);
        conn.setInstanceFollowRedirects(true);
        applyHeaders(conn, headers);
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

    private static void applyHeaders(HttpURLConnection conn, JSObject headers) {
        if (headers == null) {
            return;
        }
        Iterator<String> keys = headers.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object val = headers.opt(key);
            if (val != null && val != JSONObject.NULL) {
                conn.setRequestProperty(key, String.valueOf(val));
            }
        }
    }

    private static String sanitizeName(String name) {
        if (name == null || name.isEmpty()) {
            return "file.bin";
        }
        String leaf = name;
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (slash >= 0) {
            leaf = name.substring(slash + 1);
        }
        int dot = leaf.lastIndexOf('.');
        String base = dot > 0 ? leaf.substring(0, dot) : leaf;
        String ext = dot > 0 ? leaf.substring(dot) : "";
        base = base.replaceAll("[^A-Za-z0-9._-]", "_");
        ext = ext.replaceAll("[^A-Za-z0-9.]", "");
        if (base.replace("_", "").isEmpty()) {
            base = "file-" + System.currentTimeMillis();
        }
        if (base.length() > 60) {
            base = base.substring(0, 60);
        }
        return ext.isEmpty() ? base : base + ext;
    }
}
