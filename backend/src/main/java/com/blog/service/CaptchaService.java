package com.blog.service;

import com.blog.common.BusinessException;
import com.blog.config.BlogProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 图形验证码：Redis 优先，失败回退内存。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaService {

    private static final String REDIS_PREFIX = "captcha:";
    private static final long TTL_SECONDS = 300;
    private static final char[] CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    private final BlogProperties blogProperties;
    private final Map<String, String> memoryStore = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    public Map<String, String> create() {
        String id = UUID.randomUUID().toString().replace("-", "");
        String code = randomCode(4);
        store(id, code.toLowerCase());
        String image = renderBase64(code);
        return Map.of(
                "captchaId", id,
                "imageBase64", "data:image/png;base64," + image
        );
    }

    public void verifyOrThrow(String captchaId, String captchaCode) {
        if (!blogProperties.getSecurity().isCaptchaEnabled()) {
            return;
        }
        if (!StringUtils.hasText(captchaId) || !StringUtils.hasText(captchaCode)) {
            throw new BusinessException("请填写验证码");
        }
        String expect = take(captchaId);
        if (expect == null) {
            throw new BusinessException("验证码已过期，请刷新");
        }
        if (!expect.equalsIgnoreCase(captchaCode.trim())) {
            throw new BusinessException("验证码错误");
        }
    }

    private void store(String id, String code) {
        if (stringRedisTemplate != null) {
            try {
                stringRedisTemplate.opsForValue().set(REDIS_PREFIX + id, code, TTL_SECONDS, TimeUnit.SECONDS);
                return;
            } catch (Exception e) {
                log.debug("captcha redis store fallback: {}", e.getMessage());
            }
        }
        memoryStore.put(id, code);
    }

    private String take(String id) {
        if (stringRedisTemplate != null) {
            try {
                String key = REDIS_PREFIX + id;
                String val = stringRedisTemplate.opsForValue().get(key);
                if (val != null) {
                    stringRedisTemplate.delete(key);
                    return val;
                }
            } catch (Exception e) {
                log.debug("captcha redis get fallback: {}", e.getMessage());
            }
        }
        return memoryStore.remove(id);
    }

    private String randomCode(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(CHARS[(int) (Math.random() * CHARS.length)]);
        }
        return sb.toString();
    }

    private String renderBase64(String code) {
        int w = 120;
        int h = 40;
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(243, 239, 230));
        g.fillRect(0, 0, w, h);
        g.setFont(new Font("Arial", Font.BOLD, 24));
        for (int i = 0; i < code.length(); i++) {
            g.setColor(new Color(15, 106, 86));
            g.drawString(String.valueOf(code.charAt(i)), 18 + i * 24, 28);
        }
        g.setColor(new Color(92, 103, 95, 80));
        for (int i = 0; i < 5; i++) {
            g.drawLine((int) (Math.random() * w), (int) (Math.random() * h),
                    (int) (Math.random() * w), (int) (Math.random() * h));
        }
        g.dispose();
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            throw new BusinessException("验证码生成失败");
        }
    }
}
