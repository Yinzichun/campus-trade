package com.campus.trade.service;

import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 图形验证码：服务端用 Java2D 画一张图，验证码文本缓存在内存里，5 分钟过期
 */
@Service
public class CaptchaService {

    private static final String CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private final Map<String, Item> cache = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public Map<String, String> generate() {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            code.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        String key = UUID.randomUUID().toString().replace("-", "");
        cache.put(key, new Item(code.toString(), System.currentTimeMillis() + 5 * 60 * 1000));

        BufferedImage image = new BufferedImage(120, 40, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(245, 247, 250));
        g.fillRect(0, 0, 120, 40);
        // 干扰线
        for (int i = 0; i < 6; i++) {
            g.setColor(new Color(random.nextInt(160) + 60, random.nextInt(160) + 60, random.nextInt(160) + 60));
            g.drawLine(random.nextInt(120), random.nextInt(40), random.nextInt(120), random.nextInt(40));
        }
        // 字符
        for (int i = 0; i < 4; i++) {
            g.setColor(new Color(random.nextInt(90) + 30, random.nextInt(90) + 30, random.nextInt(90) + 30));
            g.setFont(new Font("SansSerif", Font.BOLD, 26));
            double theta = (random.nextInt(50) - 25) * Math.PI / 180;
            g.rotate(theta, 20 + i * 25, 27);
            g.drawString(String.valueOf(code.charAt(i)), 14 + i * 26, 30);
            g.rotate(-theta, 20 + i * 25, 27);
        }
        g.dispose();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", out);
        } catch (Exception e) {
            throw new RuntimeException("验证码生成失败", e);
        }
        String base64 = "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
        return Map.of("captchaKey", key, "image", base64);
    }

    /**
     * 校验并立即失效，防止同一个验证码重复使用
     */
    public boolean verify(String key, String code) {
        if (key == null || code == null) {
            return false;
        }
        Item item = cache.remove(key);
        if (item == null || item.expireAt < System.currentTimeMillis()) {
            return false;
        }
        return item.code.equalsIgnoreCase(code.trim());
    }

    private static class Item {
        private final String code;
        private final long expireAt;

        Item(String code, long expireAt) {
            this.code = code;
            this.expireAt = expireAt;
        }
    }
}
