package com.auction.business.controller;

import com.auction.business.dto.NotificationQueryDTO;
import com.auction.business.entity.BizNotification;
import com.auction.business.mapper.BizNotificationMapper;
import com.auction.business.vo.NotificationVO;
import com.auction.common.core.Result;
import com.auction.common.exception.BizException;
import com.auction.framework.security.SecurityUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 当前用户站内信接口。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/me/notifications")
public class NotificationController {

    private final BizNotificationMapper notificationMapper;

    @GetMapping
    public Result<IPage<NotificationVO>> list(NotificationQueryDTO query) {
        Long userId = SecurityUtils.getUserId();
        LambdaQueryWrapper<BizNotification> wrapper = new LambdaQueryWrapper<BizNotification>()
                .eq(BizNotification::getUserId, userId)
                .orderByAsc(BizNotification::getIsRead)
                .orderByDesc(BizNotification::getCreatedAt);
        if (query.getIsRead() != null) {
            wrapper.eq(BizNotification::getIsRead, query.getIsRead());
        }
        Page<BizNotification> page = new Page<>(query.getPage(), query.getSize());
        return Result.success(notificationMapper.selectPage(page, wrapper).convert(this::toVO));
    }

    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        Long userId = SecurityUtils.getUserId();
        BizNotification notification = notificationMapper.selectById(id);
        if (notification == null || !userId.equals(notification.getUserId())) {
            throw new BizException(70001, "站内信不存在");
        }
        if (!Integer.valueOf(1).equals(notification.getIsRead())) {
            BizNotification update = new BizNotification();
            update.setId(id);
            update.setIsRead(1);
            update.setReadAt(LocalDateTime.now());
            notificationMapper.updateById(update);
        }
        return Result.success(null);
    }

    private NotificationVO toVO(BizNotification entity) {
        NotificationVO vo = new NotificationVO();
        vo.setId(entity.getId());
        vo.setType(entity.getType());
        vo.setTitle(entity.getTitle());
        vo.setContent(entity.getContent());
        vo.setRelatedItemId(entity.getRelatedItemId());
        vo.setIsRead(entity.getIsRead());
        vo.setReadAt(entity.getReadAt());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
