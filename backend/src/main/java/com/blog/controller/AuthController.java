package com.blog.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.common.ApiResponse;
import com.blog.common.BusinessException;
import com.blog.dto.LoginRequest;
import com.blog.dto.PasswordChangeRequest;
import com.blog.entity.User;
import com.blog.mapper.UserMapper;
import com.blog.security.JwtUtil;
import com.blog.service.CaptchaService;
import com.blog.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RateLimitService rateLimitService;
    private final CaptchaService captchaService;

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody LoginRequest req,
                                                  HttpServletRequest request) {
        rateLimitService.check("admin-login:ip:" + clientIp(request), 20, 3600_000);
        captchaService.verifyOrThrow(req.getCaptchaId(), req.getCaptchaCode());
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, req.getUsername()));
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("token", token);
        map.put("username", user.getUsername());
        map.put("nickname", user.getNickname() == null ? user.getUsername() : user.getNickname());
        map.put("mustChangePassword", Integer.valueOf(1).equals(user.getMustChangePassword()));
        return ApiResponse.ok(map);
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me(org.springframework.security.core.Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("nickname", user.getNickname() == null ? user.getUsername() : user.getNickname());
        map.put("mustChangePassword", Integer.valueOf(1).equals(user.getMustChangePassword()));
        return ApiResponse.ok(map);
    }

    @PostMapping({"/password", "/change-password"})
    public ApiResponse<Void> changePassword(@Valid @RequestBody PasswordChangeRequest req,
                                            org.springframework.security.core.Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        User db = userMapper.selectById(user.getId());
        if (!passwordEncoder.matches(req.getOldPassword(), db.getPassword())) {
            throw new BusinessException("原密码不正确");
        }
        db.setPassword(passwordEncoder.encode(req.getNewPassword()));
        db.setMustChangePassword(0);
        userMapper.updateById(db);
        return ApiResponse.ok();
    }

    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
