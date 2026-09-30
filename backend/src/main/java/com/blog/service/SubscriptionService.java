package com.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.common.BusinessException;
import com.blog.entity.Category;
import com.blog.entity.MemberCategorySub;
import com.blog.mapper.CategoryMapper;
import com.blog.mapper.MemberCategorySubMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final MemberCategorySubMapper subMapper;
    private final CategoryMapper categoryMapper;

    @Transactional
    public void subscribe(Long memberId, Long categoryId) {
        Category c = categoryMapper.selectById(categoryId);
        if (c == null) {
            throw new BusinessException("专题不存在");
        }
        Long exists = subMapper.selectCount(new LambdaQueryWrapper<MemberCategorySub>()
                .eq(MemberCategorySub::getMemberId, memberId)
                .eq(MemberCategorySub::getCategoryId, categoryId));
        if (exists != null && exists > 0) {
            return;
        }
        MemberCategorySub sub = new MemberCategorySub();
        sub.setMemberId(memberId);
        sub.setCategoryId(categoryId);
        subMapper.insert(sub);
    }

    @Transactional
    public void unsubscribe(Long memberId, Long categoryId) {
        subMapper.delete(new LambdaQueryWrapper<MemberCategorySub>()
                .eq(MemberCategorySub::getMemberId, memberId)
                .eq(MemberCategorySub::getCategoryId, categoryId));
    }

    public List<Category> list(Long memberId) {
        List<MemberCategorySub> subs = subMapper.selectList(new LambdaQueryWrapper<MemberCategorySub>()
                .eq(MemberCategorySub::getMemberId, memberId)
                .orderByDesc(MemberCategorySub::getCreatedAt));
        return subs.stream()
                .map(s -> categoryMapper.selectById(s.getCategoryId()))
                .filter(c -> c != null)
                .toList();
    }
}
