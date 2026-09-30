package com.blog.security;

import com.blog.config.BlogProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-GCM 响应体加密。前端持有同一密钥解密。
 * 注意：密钥会出现在前端构建产物中，属于防窥/防简单爬取，不是绝对保密。
 */
@Component
@RequiredArgsConstructor
public class ResponseCryptoService {

    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_BITS = 128;

    private final BlogProperties blogProperties;
    private final SecureRandom secureRandom = new SecureRandom();
    private SecretKeySpec keySpec;

    @PostConstruct
    void init() {
        refreshKey();
    }

    public void refreshKey() {
        String raw = blogProperties.getSecurity().getResponseAesKey();
        if (!StringUtils.hasText(raw)) {
            raw = "BlogAesKey-ChangeInProd-32b!";
        }
        byte[] keyBytes = raw.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
            // 归一到 32 字节
            byte[] normalized = new byte[32];
            System.arraycopy(keyBytes, 0, normalized, 0, Math.min(keyBytes.length, 32));
            keyBytes = normalized;
        }
        this.keySpec = new SecretKeySpec(keyBytes, "AES");
    }

    public boolean enabled() {
        return blogProperties.getSecurity().isEncryptResponse();
    }

    public String encryptToBase64(String plaintext) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] cipherText = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buf = ByteBuffer.allocate(iv.length + cipherText.length);
            buf.put(iv);
            buf.put(cipherText);
            return Base64.getEncoder().encodeToString(buf.array());
        } catch (Exception e) {
            throw new IllegalStateException("响应加密失败: " + e.getMessage(), e);
        }
    }
}
