package com.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.common.BusinessException;
import com.blog.common.PageResult;
import com.blog.dto.CommentAdminVO;
import com.blog.dto.CommentRequest;
import com.blog.entity.Article;
import com.blog.entity.Comment;
import com.blog.entity.Member;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.CommentMapper;
import com.blog.mapper.MemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentMapper commentMapper;
    private final ArticleMapper articleMapper;
    private final MemberMapper memberMapper;
    private final NotificationService notificationService;
    private final RateLimitService rateLimitService;

    public void submit(CommentRequest req, Member member, String ip) {
        if (member == null || member.getId() == null) {
            throw new BusinessException(401, "请先登录后再评论");
        }
        rateLimitService.check("comment:ip:" + (ip == null ? "unknown" : ip), 20, 60_000);
        rateLimitService.check("comment:m:" + member.getId(), 10, 60_000);

        Article article = articleMapper.selectById(req.getArticleId());
        if (article == null || !Integer.valueOf(1).equals(article.getStatus())) {
            throw new BusinessException("文章不存在或未发布");
        }
        String content = req.getContent().trim();
        if (containsSensitive(content)) {
            throw new BusinessException("评论包含不当内容");
        }

        Long parentId = req.getParentId() == null ? 0L : req.getParentId();
        Long replyToMemberId = null;
        if (parentId > 0) {
            Comment parent = commentMapper.selectById(parentId);
            if (parent == null || !parent.getArticleId().equals(req.getArticleId())) {
                throw new BusinessException("父评论不存在");
            }
            replyToMemberId = parent.getMemberId();
        }

        Comment c = new Comment();
        c.setArticleId(req.getArticleId());
        c.setParentId(parentId);
        c.setMemberId(member.getId());
        c.setReplyToMemberId(replyToMemberId);
        c.setNickname(member.getNickname());
        c.setEmail(member.getEmail());
        c.setAvatar(member.getAvatar());
        c.setContent(content);
        c.setStatus(0);
        c.setIp(maskIp(ip));
        commentMapper.insert(c);
    }

    public PageResult<Comment> listApproved(Long articleId, long page, long size) {
        Page<Comment> p = commentMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getArticleId, articleId)
                        .eq(Comment::getStatus, 1)
                        .orderByAsc(Comment::getCreatedAt));
        // 前台脱敏：不暴露邮箱与 IP
        List<Comment> records = p.getRecords().stream().map(c -> {
            Comment view = new Comment();
            view.setId(c.getId());
            view.setArticleId(c.getArticleId());
            view.setParentId(c.getParentId());
            view.setMemberId(c.getMemberId());
            view.setReplyToMemberId(c.getReplyToMemberId());
            view.setNickname(c.getNickname());
            view.setAvatar(c.getAvatar());
            view.setContent(c.getContent());
            view.setStatus(c.getStatus());
            view.setCreatedAt(c.getCreatedAt());
            return view;
        }).toList();
        return new PageResult<>(records, p.getTotal(), page, size);
    }

    public PageResult<CommentAdminVO> pageAdmin(long page, long size, Integer status, Long memberId, String nickname) {
        LambdaQueryWrapper<Comment> qw = new LambdaQueryWrapper<Comment>()
                .eq(status != null, Comment::getStatus, status)
                .eq(memberId != null, Comment::getMemberId, memberId)
                .like(StringUtils.hasText(nickname), Comment::getNickname, nickname)
                .orderByDesc(Comment::getCreatedAt);
        Page<Comment> p = commentMapper.selectPage(new Page<>(page, size), qw);
        List<CommentAdminVO> vos = p.getRecords().stream().map(this::toAdminVO).toList();
        return new PageResult<>(vos, p.getTotal(), page, size);
    }

    public void updateStatus(Long id, Integer status) {
        Comment c = commentMapper.selectById(id);
        if (c == null) {
            throw new BusinessException("评论不存在");
        }
        Integer old = c.getStatus();
        c.setStatus(status);
        commentMapper.updateById(c);

        // 回复审核通过：通知父评论作者
        if (Integer.valueOf(1).equals(status)
                && !Integer.valueOf(1).equals(old)
                && c.getParentId() != null
                && c.getParentId() > 0
                && c.getReplyToMemberId() != null
                && !c.getReplyToMemberId().equals(c.getMemberId())) {
            notificationService.notify(
                    c.getReplyToMemberId(),
                    "comment_reply",
                    "收到新回复",
                    c.getNickname() + " 回复了你的评论：" + abbreviate(c.getContent(), 80),
                    c.getId());
        }
    }

    public void delete(Long id) {
        commentMapper.deleteById(id);
    }

    private CommentAdminVO toAdminVO(Comment c) {
        CommentAdminVO vo = new CommentAdminVO();
        BeanUtils.copyProperties(c, vo);
        if (c.getMemberId() != null) {
            Member m = memberMapper.selectById(c.getMemberId());
            if (m != null) {
                vo.setMemberPhone(m.getPhone());
            }
        }
        return vo;
    }

    private String abbreviate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    private boolean containsSensitive(String content) {
        String[] words = {"傻逼", "操你", "色情", "赌博"};
        String lower = content.toLowerCase();
        for (String w : words) {
            if (lower.contains(w)) {
                return true;
            }
        }
        return false;
    }

    private String maskIp(String ip) {
        if (ip == null) {
            return "unknown";
        }
        if (ip.contains(".")) {
            String[] parts = ip.split("\\.");
            if (parts.length == 4) {
                return parts[0] + "." + parts[1] + ".*.*";
            }
        }
        return ip.length() > 8 ? ip.substring(0, 8) + "***" : ip;
    }
}
