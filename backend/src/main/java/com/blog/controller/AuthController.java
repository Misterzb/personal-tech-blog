package com.blog.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.common.ApiResponse;
import com.blog.common.BusinessException;
import com.blog.dto.LoginRequest;
import com.blog.entity.User;
import com.blog.mapper.UserMapper;
import com.blog.security.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody LoginRequest req) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, req.getUsername()));
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        return ApiResponse.ok(Map.of(
                "token", token,
                "username", user.getUsername(),
                "nickname", user.getNickname() == null ? user.getUsername() : user.getNickname()
        ));
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me(org.springframework.security.core.Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ApiResponse.ok(Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "nickname", user.getNickname() == null ? user.getUsername() : user.getNickname()
        ));
    }

    @PostMapping("/password")
    public ApiResponse<Void> changePassword(@RequestBody Map<String, String> body,
                                            org.springframework.security.core.Authentication authentication) {
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        if (oldPassword == null || newPassword == null || newPassword.length() < 6) {
            throw new BusinessException("新密码至少 6 位");
        }
        User user = (User) authentication.getPrincipal();
        User db = userMapper.selectById(user.getId());
        if (!passwordEncoder.matches(oldPassword, db.getPassword())) {
            throw new BusinessException("原密码不正确");
        }
        db.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(db);
        return ApiResponse.ok();
    }
}
