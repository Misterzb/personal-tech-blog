package com.blog.service;

import com.blog.config.BlogProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 错误告警：ERROR 级异常通过 Webhook（企业微信/钉钉/自定义）推送。
 * 带简单冷却，避免刷屏。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ErrorAlertService {

    private final BlogProperties blogProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();
    private final AtomicLong lastSentAt = new AtomicLong(0);

    @Async
    public void notifyError(String source, String message, Throwable error) {
        BlogProperties.Alert alert = blogProperties.getAlert();
        if (alert == null || !alert.isEnabled() || !StringUtils.hasText(alert.getWebhookUrl())) {
            return;
        }
        long now = System.currentTimeMillis();
        long cooldownMs = Math.max(10_000L, alert.getCooldownSeconds() * 1000L);
        long prev = lastSentAt.get();
        if (now - prev < cooldownMs) {
            return;
        }
        if (!lastSentAt.compareAndSet(prev, now) && now - lastSentAt.get() < cooldownMs) {
            return;
        }
        try {
            String text = buildText(source, message, error);
            String body = buildPayload(alert.getWebhookType(), text);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(alert.getWebhookUrl()))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 300) {
                log.warn("Alert webhook HTTP {}: {}", resp.statusCode(), resp.body());
            }
        } catch (Exception e) {
            log.warn("Alert webhook failed: {}", e.getMessage());
        }
    }

    private String buildText(String source, String message, Throwable error) {
        StringBuilder sb = new StringBuilder();
        sb.append("【博客告警】").append(LocalDateTime.now()).append('\n');
        sb.append("来源: ").append(source == null ? "-" : source).append('\n');
        sb.append("信息: ").append(message == null ? "-" : message).append('\n');
        if (error != null) {
            sb.append("异常: ").append(error.getClass().getSimpleName())
                    .append(" - ").append(error.getMessage());
        }
        return sb.toString();
    }

    private String buildPayload(String type, String text) throws Exception {
        Map<String, Object> map = new LinkedHashMap<>();
        if ("dingtalk".equalsIgnoreCase(type)) {
            map.put("msgtype", "text");
            map.put("text", Map.of("content", text));
        } else if ("feishu".equalsIgnoreCase(type) || "lark".equalsIgnoreCase(type)) {
            map.put("msg_type", "text");
            map.put("content", Map.of("text", text));
        } else {
            // 企业微信默认
            map.put("msgtype", "text");
            map.put("text", Map.of("content", text));
        }
        return objectMapper.writeValueAsString(map);
    }
}
