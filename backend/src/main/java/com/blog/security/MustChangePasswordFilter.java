package com.blog.security;

import com.blog.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

/**
 * 管理员强制改密：除登录与改密接口外，拦截其它 /api/admin/** 请求。
 */
@Component
public class MustChangePasswordFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path != null && path.startsWith("/api/admin/")
                && !path.equals("/api/admin/auth/login")
                && !path.equals("/api/admin/auth/password")
                && !path.equals("/api/admin/auth/change-password")
                && !path.equals("/api/admin/auth/me")) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof User user
                    && Integer.valueOf(1).equals(user.getMustChangePassword())) {
                response.setStatus(403);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding("UTF-8");
                objectMapper.writeValue(response.getWriter(), Map.of(
                        "code", 403,
                        "message", "请先修改默认密码",
                        "data", Map.of("mustChangePassword", true)
                ));
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
