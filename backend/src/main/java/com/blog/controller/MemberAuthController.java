package com.blog.controller;

import com.blog.common.ApiResponse;
import com.blog.common.BusinessException;
import com.blog.common.PageResult;
import com.blog.dto.MemberLoginRequest;
import com.blog.dto.MemberRegisterRequest;
import com.blog.dto.PasswordChangeRequest;
import com.blog.dto.ProfileUpdateRequest;
import com.blog.dto.ReadingProgressRequest;
import com.blog.entity.Category;
import com.blog.entity.Comment;
import com.blog.entity.Member;
import com.blog.entity.MemberNotification;
import com.blog.service.FavoriteService;
import com.blog.service.MemberService;
import com.blog.service.NotificationService;
import com.blog.service.CaptchaService;
import com.blog.service.RateLimitService;
import com.blog.service.ReadingProgressService;
import com.blog.service.SubscriptionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class MemberAuthController {

    private final MemberService memberService;
    private final NotificationService notificationService;
    private final ReadingProgressService readingProgressService;
    private final FavoriteService favoriteService;
    private final SubscriptionService subscriptionService;
    private final RateLimitService rateLimitService;
    private final CaptchaService captchaService;

    @PostMapping("/register")
    public ApiResponse<Map<String, Object>> register(@Valid @RequestBody MemberRegisterRequest req,
                                                     HttpServletRequest request) {
        rateLimitService.check("register:ip:" + clientIp(request), 10, 3600_000);
        captchaService.verifyOrThrow(req.getCaptchaId(), req.getCaptchaCode());
        return ApiResponse.ok(memberService.register(req));
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody MemberLoginRequest req,
                                                  HttpServletRequest request) {
        rateLimitService.check("login:ip:" + clientIp(request), 30, 3600_000);
        captchaService.verifyOrThrow(req.getCaptchaId(), req.getCaptchaCode());
        return ApiResponse.ok(memberService.login(req));
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me(Authentication authentication) {
        return ApiResponse.ok(memberService.profile(requireMember(authentication)));
    }

    @PutMapping("/profile")
    public ApiResponse<Map<String, Object>> updateProfile(@Valid @RequestBody ProfileUpdateRequest req,
                                                          Authentication authentication) {
        Member m = requireMember(authentication);
        return ApiResponse.ok(memberService.updateProfile(m.getId(), req));
    }

    @PutMapping("/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody PasswordChangeRequest req,
                                            Authentication authentication) {
        Member m = requireMember(authentication);
        memberService.changePassword(m.getId(), req);
        return ApiResponse.ok();
    }

    @PostMapping("/avatar")
    public ApiResponse<Map<String, Object>> avatar(@RequestParam("file") MultipartFile file,
                                                   Authentication authentication) {
        Member m = requireMember(authentication);
        return ApiResponse.ok(memberService.uploadAvatar(m.getId(), file));
    }

    @GetMapping("/my-comments")
    public ApiResponse<PageResult<Comment>> myComments(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            Authentication authentication) {
        Member m = requireMember(authentication);
        return ApiResponse.ok(memberService.myComments(m.getId(), page, size));
    }

    @GetMapping("/notifications")
    public ApiResponse<PageResult<MemberNotification>> notifications(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            Authentication authentication) {
        Member m = requireMember(authentication);
        return ApiResponse.ok(notificationService.page(m.getId(), page, size));
    }

    @PutMapping("/notifications/{id}/read")
    public ApiResponse<Void> markRead(@PathVariable Long id, Authentication authentication) {
        Member m = requireMember(authentication);
        notificationService.markRead(m.getId(), id);
        return ApiResponse.ok();
    }

    @PutMapping("/notifications/read-all")
    public ApiResponse<Void> markAllRead(Authentication authentication) {
        Member m = requireMember(authentication);
        notificationService.markAllRead(m.getId());
        return ApiResponse.ok();
    }

    @GetMapping("/notifications/unread-count")
    public ApiResponse<Map<String, Long>> unreadCount(Authentication authentication) {
        Member m = requireMember(authentication);
        return ApiResponse.ok(Map.of("count", notificationService.unreadCount(m.getId())));
    }

    @PostMapping("/reading-progress")
    public ApiResponse<Map<String, Object>> saveProgress(@Valid @RequestBody ReadingProgressRequest req,
                                                         Authentication authentication) {
        Member m = requireMember(authentication);
        return ApiResponse.ok(readingProgressService.save(m.getId(), req));
    }

    @GetMapping("/reading-progress")
    public ApiResponse<Map<String, Object>> getProgress(@RequestParam Long categoryId,
                                                        Authentication authentication) {
        Member m = requireMember(authentication);
        return ApiResponse.ok(readingProgressService.get(m.getId(), categoryId));
    }

    @PostMapping("/favorites/{articleId}")
    public ApiResponse<Void> addFavorite(@PathVariable Long articleId, Authentication authentication) {
        Member m = requireMember(authentication);
        favoriteService.add(m.getId(), articleId);
        return ApiResponse.ok();
    }

    @DeleteMapping("/favorites/{articleId}")
    public ApiResponse<Void> removeFavorite(@PathVariable Long articleId, Authentication authentication) {
        Member m = requireMember(authentication);
        favoriteService.remove(m.getId(), articleId);
        return ApiResponse.ok();
    }

    @GetMapping("/favorites")
    public ApiResponse<List<?>> favorites(Authentication authentication) {
        Member m = requireMember(authentication);
        return ApiResponse.ok(favoriteService.list(m.getId()));
    }

    @PostMapping("/subscriptions/{categoryId}")
    public ApiResponse<Void> subscribe(@PathVariable Long categoryId, Authentication authentication) {
        Member m = requireMember(authentication);
        subscriptionService.subscribe(m.getId(), categoryId);
        return ApiResponse.ok();
    }

    @DeleteMapping("/subscriptions/{categoryId}")
    public ApiResponse<Void> unsubscribe(@PathVariable Long categoryId, Authentication authentication) {
        Member m = requireMember(authentication);
        subscriptionService.unsubscribe(m.getId(), categoryId);
        return ApiResponse.ok();
    }

    @GetMapping("/subscriptions")
    public ApiResponse<List<Category>> subscriptions(Authentication authentication) {
        Member m = requireMember(authentication);
        return ApiResponse.ok(subscriptionService.list(m.getId()));
    }

    private Member requireMember(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Member member)) {
            throw new BusinessException(401, "请先登录");
        }
        if (member.getStatus() != null && member.getStatus() == 0) {
            throw new BusinessException(403, "账号已被禁用");
        }
        return member;
    }

    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
