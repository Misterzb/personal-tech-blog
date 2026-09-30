package com.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.common.BusinessException;
import com.blog.entity.FriendLink;
import com.blog.mapper.FriendLinkMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FriendLinkService {

    private final FriendLinkMapper friendLinkMapper;

    @Cacheable(cacheNames = "friendlinks", unless = "#result == null || #result.isEmpty()")
    public List<FriendLink> listActive() {
        return friendLinkMapper.selectList(new LambdaQueryWrapper<FriendLink>()
                .eq(FriendLink::getStatus, 1)
                .orderByAsc(FriendLink::getSortOrder)
                .orderByDesc(FriendLink::getId));
    }

    public List<FriendLink> listAll() {
        return friendLinkMapper.selectList(new LambdaQueryWrapper<FriendLink>()
                .orderByAsc(FriendLink::getSortOrder)
                .orderByDesc(FriendLink::getId));
    }

    @CacheEvict(cacheNames = "friendlinks", allEntries = true)
    public FriendLink save(FriendLink link) {
        if (!StringUtils.hasText(link.getName()) || !StringUtils.hasText(link.getUrl())) {
            throw new BusinessException("名称和链接不能为空");
        }
        if (link.getStatus() == null) {
            link.setStatus(1);
        }
        if (link.getSortOrder() == null) {
            link.setSortOrder(0);
        }
        if (link.getId() == null) {
            friendLinkMapper.insert(link);
        } else {
            friendLinkMapper.updateById(link);
        }
        return link;
    }

    @CacheEvict(cacheNames = "friendlinks", allEntries = true)
    public void delete(Long id) {
        friendLinkMapper.deleteById(id);
    }
}
