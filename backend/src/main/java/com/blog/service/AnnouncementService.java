package com.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.common.BusinessException;
import com.blog.entity.Announcement;
import com.blog.mapper.AnnouncementMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final AnnouncementMapper announcementMapper;

    @Cacheable(cacheNames = "announcements", unless = "#result == null || #result.isEmpty()")
    public List<Announcement> listActive() {
        return announcementMapper.selectList(new LambdaQueryWrapper<Announcement>()
                .eq(Announcement::getStatus, 1)
                .orderByAsc(Announcement::getSortOrder)
                .orderByDesc(Announcement::getId));
    }

    public List<Announcement> listAll() {
        return announcementMapper.selectList(new LambdaQueryWrapper<Announcement>()
                .orderByAsc(Announcement::getSortOrder)
                .orderByDesc(Announcement::getId));
    }

    @CacheEvict(cacheNames = "announcements", allEntries = true)
    public Announcement save(Announcement a) {
        if (!StringUtils.hasText(a.getTitle())) {
            throw new BusinessException("标题不能为空");
        }
        if (a.getStatus() == null) {
            a.setStatus(1);
        }
        if (a.getSortOrder() == null) {
            a.setSortOrder(0);
        }
        if (a.getId() == null) {
            announcementMapper.insert(a);
        } else {
            announcementMapper.updateById(a);
        }
        return a;
    }

    @CacheEvict(cacheNames = "announcements", allEntries = true)
    public void delete(Long id) {
        announcementMapper.deleteById(id);
    }
}
