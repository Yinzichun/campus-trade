package com.campus.trade.common;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 轻量 Token 工具：payload 用 Base64Url 编码，末尾拼接 HMAC-SHA256 签名，
 * 防止前端伪造 userId。有效期 7 天。
 */
public class TokenUtil {

    private static final String SECRET = "campus-trade-2026";
    private static final long EXPIRE = 7L * 24 * 3600 * 1000;

    public static String create(Long userId) {
        String payload = userId + ":" + (System.currentTimeMillis() + EXPIRE);
        String body = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return body + "." + sign(payload);
    }

    public static Long parse(String token) {
        if (token == null || !token.contains(".")) {
            return null;
        }
        String[] parts = token.split("\\.");
        if (parts.length != 2) {
            return null;
        }
        String payload;
        try {
            payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return null;
        }
        if (!sign(payload).equals(parts[1])) {
            return null;
        }
        String[] fields = payload.split(":");
        if (fields.length != 2) {
            return null;
        }
        if (Long.parseLong(fields[1]) < System.currentTimeMillis()) {
            return null;
        }
        return Long.parseLong(fields[0]);
    }

    private static String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("签名失败", e);
        }
    }
}
