package com.blog.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.entity.Member;
import com.blog.entity.User;
import com.blog.mapper.MemberMapper;
import com.blog.mapper.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;
    private final MemberMapper memberMapper;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtUtil.isValid(token) && SecurityContextHolder.getContext().getAuthentication() == null) {
                String role = jwtUtil.getRole(token);
                Long uid = jwtUtil.getUid(token);
                if (JwtUtil.ROLE_MEMBER.equals(role)) {
                    Member member = uid != null
                            ? memberMapper.selectById(uid)
                            : memberMapper.selectOne(new LambdaQueryWrapper<Member>()
                            .eq(Member::getPhone, jwtUtil.getUsername(token)));
                    if (member != null) {
                        if (member.getStatus() != null && member.getStatus() == 0) {
                            writeJson(response, 403, "账号已被禁用");
                            return;
                        }
                        var auth = new UsernamePasswordAuthenticationToken(
                                member, null, List.of(new SimpleGrantedAuthority("ROLE_MEMBER")));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                } else {
                    String username = jwtUtil.getUsername(token);
                    User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
                    if (user != null) {
                        var auth = new UsernamePasswordAuthenticationToken(
                                user, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                }
            }
        }
        filterChain.doFilter(request, response);
    }

    private void writeJson(HttpServletResponse response, int code, String message) throws IOException {
        response.setStatus(code == 401 ? 401 : 403);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), Map.of("code", code, "message", message, "data", null));
    }
}
