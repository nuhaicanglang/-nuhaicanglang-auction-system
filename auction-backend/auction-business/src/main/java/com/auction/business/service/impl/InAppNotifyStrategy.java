package com.auction.business.service.impl;

import com.auction.business.dto.NotifyCreateDTO;
import com.auction.business.entity.BizNotification;
import com.auction.business.mapper.BizNotificationMapper;
import com.auction.business.service.NotifyChannelStrategy;
import com.auction.common.util.SnowflakeIdWorker;
import com.auction.framework.websocket.WsPusher;
import com.auction.system.entity.SysUser;
import com.auction.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class InAppNotifyStrategy implements NotifyChannelStrategy {

    private final BizNotificationMapper notificationMapper;
    private final SysUserMapper sysUserMapper;
    private final WsPusher wsPusher;
    private final SnowflakeIdWorker idWorker = new SnowflakeIdWorker();

    @Override
    public String channel() {
        return "IN_APP";
    }

    @Override
    public void send(NotifyCreateDTO dto) {
        BizNotification notification = new BizNotification();
        notification.setId(idWorker.nextId());
        notification.setUserId(dto.getUserId());
        notification.setType(dto.getType());
        notification.setTitle(dto.getTitle());
        notification.setContent(dto.getContent());
        notification.setRelatedItemId(dto.getRelatedItemId());
        notification.setIsRead(0);
        notification.setCreatedAt(LocalDateTime.now());
        notificationMapper.insert(notification);

        SysUser user = sysUserMapper.selectById(dto.getUserId());
        if (user != null && user.getUsername() != null) {
            wsPusher.pushUserNotification(user.getUsername(), dto.getTitle(), dto.getContent());
        }
    }
}
