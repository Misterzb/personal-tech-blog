package com.blog.security;

import com.blog.config.BlogProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtUtil {

    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_MEMBER = "MEMBER";

    private final BlogProperties blogProperties;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(blogProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /** 兼容旧调用：默认 ADMIN */
    public String generateToken(Long userId, String username) {
        return generateToken(userId, username, ROLE_ADMIN);
    }

    public String generateToken(Long userId, String subject, String role) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + blogProperties.getJwt().getExpirationMs());
        return Jwts.builder()
                .subject(subject)
                .claim("uid", userId)
                .claim("role", role)
                .issuedAt(now)
                .expiration(exp)
                .signWith(key())
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getUsername(String token) {
        return parse(token).getSubject();
    }

    public String getRole(String token) {
        Object role = parse(token).get("role");
        return role == null ? ROLE_ADMIN : String.valueOf(role);
    }

    public Long getUid(String token) {
        Object uid = parse(token).get("uid");
        if (uid instanceof Number n) {
            return n.longValue();
        }
        return uid == null ? null : Long.parseLong(String.valueOf(uid));
    }

    public boolean isValid(String token) {
        try {
            Claims claims = parse(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }
}
