package com.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.common.BusinessException;
import com.blog.common.PageResult;
import com.blog.entity.MemberNotification;
import com.blog.mapper.MemberNotificationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final MemberNotificationMapper notificationMapper;

    public void notify(Long memberId, String type, String title, String content, Long relatedId) {
        if (memberId == null) {
            return;
        }
        MemberNotification n = new MemberNotification();
        n.setMemberId(memberId);
        n.setType(type == null ? "system" : type);
        n.setTitle(title);
        n.setContent(content);
        n.setRelatedId(relatedId);
        n.setIsRead(0);
        notificationMapper.insert(n);
    }

    public PageResult<MemberNotification> page(Long memberId, long page, long size) {
        Page<MemberNotification> p = notificationMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<MemberNotification>()
                        .eq(MemberNotification::getMemberId, memberId)
                        .orderByDesc(MemberNotification::getCreatedAt));
        return new PageResult<>(p.getRecords(), p.getTotal(), page, size);
    }

    public void markRead(Long memberId, Long id) {
        MemberNotification n = notificationMapper.selectById(id);
        if (n == null || !memberId.equals(n.getMemberId())) {
            throw new BusinessException("通知不存在");
        }
        n.setIsRead(1);
        notificationMapper.updateById(n);
    }

    public void markAllRead(Long memberId) {
        notificationMapper.update(null, new LambdaUpdateWrapper<MemberNotification>()
                .eq(MemberNotification::getMemberId, memberId)
                .eq(MemberNotification::getIsRead, 0)
                .set(MemberNotification::getIsRead, 1));
    }

    public long unreadCount(Long memberId) {
        Long c = notificationMapper.selectCount(new LambdaQueryWrapper<MemberNotification>()
                .eq(MemberNotification::getMemberId, memberId)
                .eq(MemberNotification::getIsRead, 0));
        return c == null ? 0 : c;
    }
}
