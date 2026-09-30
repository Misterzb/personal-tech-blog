package com.blog.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "blog")
public class BlogProperties {
    private Jwt jwt = new Jwt();
    private Upload upload = new Upload();
    private Site site = new Site();
    private Cors cors = new Cors();
    private Security security = new Security();
    private Alert alert = new Alert();

    @Data
    public static class Jwt {
        private String secret;
        private long expirationMs = 86400000L;
    }

    @Data
    public static class Upload {
        private String dir = "./uploads";
        private String urlPrefix = "/uploads";
    }

    @Data
    public static class Site {
        private String baseUrl = "http://localhost:5173";
    }

    @Data
    public static class Cors {
        private List<String> allowedOrigins = new ArrayList<>();
    }

    /**
     * 接口防护。公开文章本身可被阅读；此处侧重限流、来源校验与响应脱敏配合。
     */
    @Data
    public static class Security {
        /** 总开关 */
        private boolean enabled = true;
        /** 公开接口每 IP 每分钟次数 */
        private int publicPerMinute = 120;
        /** 搜索接口每 IP 每分钟次数 */
        private int searchPerMinute = 30;
        /** 登录/注册边缘限流每 IP 每分钟 */
        private int authPerMinute = 40;
        /**
         * 为 true 时校验 Origin/Referer 是否在 cors.allowed-origins 内。
         * 生产站建议开启；本地开发可保持 false。
         */
        private boolean enforceOrigin = false;
        /**
         * 与 enforceOrigin 联用：无 Origin 的请求是否拒绝（挡 curl/脚本直连公开 API）。
         * 注意：过严会影响健康检查与部分合法客户端。
         */
        private boolean blockMissingOrigin = false;
        /** 是否加密 ApiResponse.data（AES-GCM），前端需同密钥解密 */
        private boolean encryptResponse = true;
        /** AES 密钥，建议 16/24/32 字节字符串；生产务必修改并与前端 VITE_API_AES_KEY 一致 */
        private String responseAesKey = "BlogAesKey-ChangeInProd-32b!";
        /** 注册/登录/评论是否校验图形验证码 */
        private boolean captchaEnabled = true;
    }

    @Data
    public static class Alert {
        /** 是否启用 Webhook 告警 */
        private boolean enabled = false;
        /** 企业微信/钉钉/飞书机器人 URL */
        private String webhookUrl = "";
        /** wecom | dingtalk | feishu */
        private String webhookType = "wecom";
        /** 告警冷却秒数，防止刷屏 */
        private long cooldownSeconds = 60;
    }
}
