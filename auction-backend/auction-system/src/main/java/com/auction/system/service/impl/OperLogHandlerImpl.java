package com.auction.system.service.impl;

import com.auction.common.util.SnowflakeIdWorker;
import com.auction.framework.aspect.OperLogEvent;
import com.auction.framework.aspect.OperLogHandler;
import com.auction.system.entity.SysOperLog;
import com.auction.system.mapper.SysOperLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 操作日志持久化实现。
 * 将 OperLogEvent 转为 SysOperLog 实体并写入数据库。
 */
@Service
@RequiredArgsConstructor
public class OperLogHandlerImpl implements OperLogHandler {

    private final SysOperLogMapper operLogMapper;
    private final SnowflakeIdWorker idWorker = new SnowflakeIdWorker();

    @Override
    public void save(OperLogEvent event) {
        SysOperLog entity = new SysOperLog();
        entity.setId(idWorker.nextId());
        entity.setTraceId(event.getTraceId());
        entity.setModule(event.getModule());
        entity.setBusinessType(event.getBusinessType());
        entity.setDescription(event.getDescription());
        entity.setMethod(event.getMethod());
        entity.setRequestUrl(event.getRequestUrl());
        entity.setRequestMethod(event.getRequestMethod());
        entity.setRequestParams(event.getRequestParams());
        entity.setResponseData(event.getResponseData());
        entity.setOperUserId(event.getOperUserId());
        entity.setOperUserName(event.getOperUserName());
        entity.setOperIp(event.getOperIp());
        entity.setUserAgent(event.getUserAgent());
        entity.setStatus(event.getStatus());
        entity.setErrorMsg(event.getErrorMsg());
        entity.setCostMs(event.getCostMs());
        entity.setCreatedAt(event.getCreatedAt());
        operLogMapper.insert(entity);
    }
}
