package com.auction.system.controller;

import com.auction.common.core.Result;
import com.auction.system.dto.OperLogQueryDTO;
import com.auction.system.entity.SysOperLog;
import com.auction.system.mapper.SysOperLogMapper;
import com.auction.system.vo.SysOperLogVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端操作日志查询接口。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/logs")
public class AdminOperLogController {

    private final SysOperLogMapper operLogMapper;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<IPage<SysOperLogVO>> list(OperLogQueryDTO query) {
        LambdaQueryWrapper<SysOperLog> wrapper = new LambdaQueryWrapper<SysOperLog>()
                .orderByDesc(SysOperLog::getCreatedAt);
        if (StringUtils.hasText(query.getModule())) {
            wrapper.like(SysOperLog::getModule, query.getModule().trim());
        }
        if (StringUtils.hasText(query.getBusinessType())) {
            wrapper.eq(SysOperLog::getBusinessType, query.getBusinessType().trim());
        }
        if (StringUtils.hasText(query.getOperUserName())) {
            wrapper.like(SysOperLog::getOperUserName, query.getOperUserName().trim());
        }
        if (query.getStatus() != null) {
            wrapper.eq(SysOperLog::getStatus, query.getStatus());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(w -> w.like(SysOperLog::getDescription, keyword)
                    .or()
                    .like(SysOperLog::getRequestUrl, keyword)
                    .or()
                    .like(SysOperLog::getTraceId, keyword));
        }

        Page<SysOperLog> page = new Page<>(query.getPage(), query.getSize());
        return Result.success(operLogMapper.selectPage(page, wrapper).convert(this::toVO));
    }

    private SysOperLogVO toVO(SysOperLog entity) {
        SysOperLogVO vo = new SysOperLogVO();
        vo.setId(entity.getId());
        vo.setTraceId(entity.getTraceId());
        vo.setModule(entity.getModule());
        vo.setBusinessType(entity.getBusinessType());
        vo.setDescription(entity.getDescription());
        vo.setMethod(entity.getMethod());
        vo.setRequestUrl(entity.getRequestUrl());
        vo.setRequestMethod(entity.getRequestMethod());
        vo.setOperUserId(entity.getOperUserId());
        vo.setOperUserName(entity.getOperUserName());
        vo.setOperIp(entity.getOperIp());
        vo.setStatus(entity.getStatus());
        vo.setErrorMsg(entity.getErrorMsg());
        vo.setCostMs(entity.getCostMs());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
