package com.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.common.BusinessException;
import com.blog.common.PageResult;
import com.blog.dto.ArticleVO;
import com.blog.dto.MemberLoginRequest;
import com.blog.dto.MemberRegisterRequest;
import com.blog.dto.PasswordChangeRequest;
import com.blog.dto.ProfileUpdateRequest;
import com.blog.entity.Comment;
import com.blog.entity.Member;
import com.blog.mapper.CommentMapper;
import com.blog.mapper.MemberMapper;
import com.blog.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class MemberService {

    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final MemberMapper memberMapper;
    private final CommentMapper commentMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AvatarService avatarService;
    private final UploadService uploadService;

    @Transactional
    public Map<String, Object> register(MemberRegisterRequest req) {
        String phone = req.getPhone().trim();
        Long exists = memberMapper.selectCount(new LambdaQueryWrapper<Member>().eq(Member::getPhone, phone));
        if (exists != null && exists > 0) {
            throw new BusinessException("该手机号已注册");
        }
        String email = StringUtils.hasText(req.getEmail()) ? req.getEmail().trim() : null;
        if (email != null && !EMAIL.matcher(email).matches()) {
            throw new BusinessException("邮箱格式不正确");
        }

        Member member = new Member();
        member.setPhone(phone);
        member.setPassword(passwordEncoder.encode(req.getPassword()));
        member.setNickname(req.getNickname().trim());
        member.setEmail(email);
        member.setAvatar("");
        member.setStatus(1);
        memberMapper.insert(member);

        String avatar = avatarService.createDefaultAvatar(member.getId(), phone);
        member.setAvatar(avatar);
        memberMapper.updateById(member);

        return authPayload(member);
    }

    public Map<String, Object> login(MemberLoginRequest req) {
        Member member = memberMapper.selectOne(new LambdaQueryWrapper<Member>()
                .eq(Member::getPhone, req.getPhone().trim()));
        if (member == null || !passwordEncoder.matches(req.getPassword(), member.getPassword())) {
            throw new BusinessException(401, "手机号或密码错误");
        }
        if (member.getStatus() != null && member.getStatus() == 0) {
            throw new BusinessException(403, "账号已被禁用");
        }
        return authPayload(member);
    }

    public Map<String, Object> profile(Member member) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", member.getId());
        map.put("phone", maskPhone(member.getPhone()));
        map.put("nickname", member.getNickname());
        map.put("email", member.getEmail() == null ? "" : member.getEmail());
        map.put("avatar", member.getAvatar() == null ? "" : member.getAvatar());
        return map;
    }

    @Transactional
    public Map<String, Object> updateProfile(Long memberId, ProfileUpdateRequest req) {
        Member member = requireMember(memberId);
        String email = StringUtils.hasText(req.getEmail()) ? req.getEmail().trim() : null;
        if (email != null && !EMAIL.matcher(email).matches()) {
            throw new BusinessException("邮箱格式不正确");
        }
        member.setNickname(req.getNickname().trim());
        member.setEmail(email);
        memberMapper.updateById(member);
        return profile(member);
    }

    @Transactional
    public void changePassword(Long memberId, PasswordChangeRequest req) {
        Member member = requireMember(memberId);
        if (!passwordEncoder.matches(req.getOldPassword(), member.getPassword())) {
            throw new BusinessException("原密码不正确");
        }
        member.setPassword(passwordEncoder.encode(req.getNewPassword()));
        memberMapper.updateById(member);
    }

    @Transactional
    public Map<String, Object> uploadAvatar(Long memberId, MultipartFile file) {
        Member member = requireMember(memberId);
        String url = uploadService.uploadAvatar(file);
        member.setAvatar(url);
        memberMapper.updateById(member);
        return profile(member);
    }

    public PageResult<Comment> myComments(Long memberId, long page, long size) {
        var p = commentMapper.selectPage(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size),
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getMemberId, memberId)
                        .orderByDesc(Comment::getCreatedAt));
        return new PageResult<>(p.getRecords(), p.getTotal(), page, size);
    }

    public PageResult<Member> pageAdmin(long page, long size, String keyword, Integer status) {
        LambdaQueryWrapper<Member> qw = new LambdaQueryWrapper<Member>()
                .eq(status != null, Member::getStatus, status)
                .and(StringUtils.hasText(keyword), w -> w
                        .like(Member::getPhone, keyword)
                        .or().like(Member::getNickname, keyword)
                        .or().like(Member::getEmail, keyword))
                .orderByDesc(Member::getCreatedAt);
        var p = memberMapper.selectPage(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size), qw);
        // 后台列表也不返回密码哈希
        p.getRecords().forEach(m -> m.setPassword(null));
        return new PageResult<>(p.getRecords(), p.getTotal(), page, size);
    }

    @Transactional
    public void updateStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("status 只能为 0 或 1");
        }
        Member member = requireMember(id);
        member.setStatus(status);
        memberMapper.updateById(member);
    }

    public Member requireMember(Long id) {
        Member member = memberMapper.selectById(id);
        if (member == null) {
            throw new BusinessException("会员不存在");
        }
        return member;
    }

    private Map<String, Object> authPayload(Member member) {
        String token = jwtUtil.generateToken(member.getId(), member.getPhone(), JwtUtil.ROLE_MEMBER);
        Map<String, Object> map = profile(member);
        map.put("token", token);
        return map;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
