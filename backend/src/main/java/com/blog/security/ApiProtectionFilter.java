package com.blog.security;

import com.blog.config.BlogProperties;
import com.blog.service.RateLimitService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * 公开 API 防护：安全响应头、IP 限流、可选 Origin 白名单校验。
 * 说明：博客正文本身是公开内容，HTTPS + 限流/脱敏可降低滥用，无法杜绝合法阅读场景下的复制。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@RequiredArgsConstructor
public class ApiProtectionFilter extends OncePerRequestFilter {

    private final BlogProperties blogProperties;
    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        applySecurityHeaders(response, path != null && path.startsWith("/api/"));

        if (path != null && path.startsWith("/api/")) {
            BlogProperties.Security sec = blogProperties.getSecurity();
            if (sec.isEnabled()) {
                String ip = clientIp(request);
                try {
                    if (path.startsWith("/api/public/")) {
                        if (sec.isEnforceOrigin() && !originAllowed(request)) {
                            writeJson(response, 403, "非法来源请求");
                            return;
                        }
                        int limit = path.contains("/search")
                                ? sec.getSearchPerMinute()
                                : sec.getPublicPerMinute();
                        rateLimitService.check("pub:" + bucket(path) + ":" + ip, limit, 60_000);
                    } else if (path.startsWith("/api/auth/login") || path.startsWith("/api/auth/register")
                            || path.startsWith("/api/admin/auth/login")) {
                        rateLimitService.check("auth-edge:" + ip, sec.getAuthPerMinute(), 60_000);
                    }
                } catch (com.blog.common.BusinessException e) {
                    writeJson(response, e.getCode() > 0 ? e.getCode() : 429, e.getMessage());
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private void applySecurityHeaders(HttpServletResponse response, boolean api) {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "SAMEORIGIN");
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        response.setHeader("X-XSS-Protection", "0");
        response.setHeader("Permissions-Policy", "geolocation=(), microphone=(), camera=()");
        if (api && !response.containsHeader("Cache-Control")) {
            response.setHeader("Cache-Control", "no-store");
        }
    }

    private boolean originAllowed(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        // 非浏览器客户端（爬虫/curl）通常不带 Origin：生产开启 enforce-origin 后可挡掉一部分脚本直连
        if (!StringUtils.hasText(origin)) {
            return !blogProperties.getSecurity().isBlockMissingOrigin();
        }
        List<String> allowed = blogProperties.getCors().getAllowedOrigins();
        if (allowed == null || allowed.isEmpty()) {
            return true;
        }
        if (allowed.contains(origin)) {
            return true;
        }
        // 兼容带尾斜杠
        String normalized = origin.endsWith("/") ? origin.substring(0, origin.length() - 1) : origin;
        if (allowed.contains(normalized)) {
            return true;
        }
        // Referer 兜底（部分环境只带 Referer）
        String referer = request.getHeader("Referer");
        if (StringUtils.hasText(referer)) {
            try {
                String refOrigin = originOf(referer);
                return allowed.contains(refOrigin);
            } catch (Exception ignored) {
                return false;
            }
        }
        return false;
    }

    private String originOf(String url) {
        URI uri = URI.create(url);
        StringBuilder sb = new StringBuilder();
        sb.append(uri.getScheme()).append("://").append(uri.getHost());
        if (uri.getPort() > 0 && uri.getPort() != 80 && uri.getPort() != 443) {
            sb.append(':').append(uri.getPort());
        }
        return sb.toString();
    }

    private String bucket(String path) {
        if (path.contains("/search")) {
            return "search";
        }
        if (path.contains("/articles")) {
            return "articles";
        }
        if (path.contains("/comments")) {
            return "comments";
        }
        return "other";
    }

    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xff)) {
            return xff.split(",")[0].trim();
        }
        String real = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(real)) {
            return real.trim();
        }
        return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }

    private void writeJson(HttpServletResponse response, int code, String message) throws IOException {
        int http = code == 429 ? 429 : (code == 403 ? 403 : 400);
        response.setStatus(http);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), Map.of(
                "code", code,
                "message", message == null ? "forbidden" : message,
                "data", null
        ));
    }
}
