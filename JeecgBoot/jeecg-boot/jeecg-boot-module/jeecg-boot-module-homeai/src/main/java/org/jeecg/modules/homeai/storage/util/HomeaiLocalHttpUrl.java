package org.jeecg.modules.homeai.storage.util;

import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.oConvertUtils;

import java.net.URI;
import java.util.Locale;
import java.util.Set;

/**
 * 预览/转换附属服务只允许本机或私网，禁止把文档发到公网。
 */
public final class HomeaiLocalHttpUrl {

    private static final Set<String> LOCAL_HOSTS = Set.of(
            "localhost",
            "127.0.0.1",
            "::1",
            "[::1]",
            "0:0:0:0:0:0:0:1",
            "host.docker.internal"
    );

    private HomeaiLocalHttpUrl() {
    }

    public static boolean isLoopbackOrPrivate(String url) {
        if (oConvertUtils.isEmpty(url)) {
            return false;
        }
        URI uri;
        try {
            uri = URI.create(url.trim());
        } catch (Exception e) {
            return false;
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            return false;
        }
        if (oConvertUtils.isNotEmpty(uri.getUserInfo())) {
            return false;
        }
        String host = uri.getHost();
        if (oConvertUtils.isEmpty(host)) {
            return false;
        }
        String h = host.toLowerCase(Locale.ROOT);
        if (h.startsWith("[") && h.endsWith("]")) {
            h = h.substring(1, h.length() - 1);
        }
        if (LOCAL_HOSTS.contains(h)) {
            return true;
        }
        return isPrivateIpv4(h);
    }

    public static void requireLoopbackOrPrivate(String url, String label) {
        if (oConvertUtils.isEmpty(url)) {
            return;
        }
        if (!isLoopbackOrPrivate(url)) {
            throw new JeecgBootException(label + "只能是本机或局域网地址（127.0.0.1 / localhost / 192.168.x / 10.x），禁止公网服务");
        }
    }

    private static boolean isPrivateIpv4(String host) {
        String[] parts = host.split("\\.");
        if (parts.length != 4) {
            return false;
        }
        int[] n = new int[4];
        try {
            for (int i = 0; i < 4; i++) {
                n[i] = Integer.parseInt(parts[i]);
                if (n[i] < 0 || n[i] > 255) {
                    return false;
                }
            }
        } catch (NumberFormatException e) {
            return false;
        }
        int a = n[0];
        int b = n[1];
        if (a == 10) {
            return true;
        }
        if (a == 192 && b == 168) {
            return true;
        }
        if (a == 172 && b >= 16 && b <= 31) {
            return true;
        }
        return a == 127;
    }
}
