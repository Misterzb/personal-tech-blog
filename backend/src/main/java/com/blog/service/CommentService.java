package com.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.common.BusinessException;
import com.blog.common.PageResult;
import com.blog.dto.CommentRequest;
import com.blog.entity.Article;
import com.blog.entity.Comment;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.CommentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentMapper commentMapper;
    private final ArticleMapper articleMapper;
    private final Map<String, LocalDateTime> rateLimit = new ConcurrentHashMap<>();

    public void submit(CommentRequest req, String ip) {
        Article article = articleMapper.selectById(req.getArticleId());
        if (article == null || !Integer.valueOf(1).equals(article.getStatus())) {
            throw new BusinessException("文章不存在或未发布");
        }
        String key = ip + ":" + req.getArticleId();
        LocalDateTime last = rateLimit.get(key);
        if (last != null && last.plusSeconds(30).isAfter(LocalDateTime.now())) {
            throw new BusinessException("评论过于频繁，请稍后再试");
        }
        String content = req.getContent().trim();
        if (containsSensitive(content)) {
            throw new BusinessException("评论包含不当内容");
        }
        Comment c = new Comment();
        c.setArticleId(req.getArticleId());
        c.setParentId(req.getParentId() == null ? 0L : req.getParentId());
        c.setNickname(req.getNickname().trim());
        c.setEmail(req.getEmail());
        c.setContent(content);
        c.setStatus(0);
        c.setIp(maskIp(ip));
        commentMapper.insert(c);
        rateLimit.put(key, LocalDateTime.now());
    }

    public PageResult<Comment> listApproved(Long articleId, long page, long size) {
        Page<Comment> p = commentMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getArticleId, articleId)
                        .eq(Comment::getStatus, 1)
                        .orderByAsc(Comment::getCreatedAt));
        return new PageResult<>(p.getRecords(), p.getTotal(), page, size);
    }

    public PageResult<Comment> pageAdmin(long page, long size, Integer status) {
        Page<Comment> p = commentMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Comment>()
                        .eq(status != null, Comment::getStatus, status)
                        .orderByDesc(Comment::getCreatedAt));
        return new PageResult<>(p.getRecords(), p.getTotal(), page, size);
    }

    public void updateStatus(Long id, Integer status) {
        Comment c = commentMapper.selectById(id);
        if (c == null) {
            throw new BusinessException("评论不存在");
        }
        c.setStatus(status);
        commentMapper.updateById(c);
    }

    public void delete(Long id) {
        commentMapper.deleteById(id);
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
