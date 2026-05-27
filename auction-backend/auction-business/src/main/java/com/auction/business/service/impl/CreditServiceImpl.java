package com.auction.business.service.impl;

import com.auction.business.dto.CreditAdjustDTO;
import com.auction.business.dto.CreditApplyCmd;
import com.auction.business.dto.CreditLogQueryDTO;
import com.auction.business.entity.BizCredit;
import com.auction.business.entity.BizCreditLog;
import com.auction.business.mapper.BizCreditLogMapper;
import com.auction.business.mapper.BizCreditMapper;
import com.auction.business.service.CreditService;
import com.auction.business.vo.CreditLogVO;
import com.auction.business.vo.CreditVO;
import com.auction.common.exception.BizException;
import com.auction.common.util.SnowflakeIdWorker;
import com.auction.framework.redis.RedisKey;
import com.auction.system.entity.SysUser;
import com.auction.system.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreditServiceImpl implements CreditService {

    private static final int DEFAULT_SCORE = 80;
    private static final int MAX_SCORE = 100;
    private static final int MIN_SCORE = 0;
    private static final int DISABLE_THRESHOLD = 30;
    private static final int DAILY_POSITIVE_LIMIT = 5;

    private final BizCreditMapper creditMapper;
    private final BizCreditLogMapper creditLogMapper;
    private final SysUserMapper sysUserMapper;
    private final RedissonClient redissonClient;
    private final StringRedisTemplate redisTemplate;
    private final SnowflakeIdWorker idWorker = new SnowflakeIdWorker();

    @Override
    public CreditVO getCredit(Long userId) {
        return toVO(getOrCreateCredit(userId));
    }

    @Override
    public IPage<CreditLogVO> listMyLogs(Long userId, CreditLogQueryDTO query) {
        query.setUserId(userId);
        return listAllLogs(query);
    }

    @Override
    public IPage<CreditLogVO> listAllLogs(CreditLogQueryDTO query) {
        LambdaQueryWrapper<BizCreditLog> wrapper = new LambdaQueryWrapper<>();
        if (query.getUserId() != null) {
            wrapper.eq(BizCreditLog::getUserId, query.getUserId());
        }
        if (StringUtils.hasText(query.getEventType())) {
            wrapper.eq(BizCreditLog::getEventType, query.getEventType());
        }
        wrapper.orderByDesc(BizCreditLog::getCreatedAt);
        Page<BizCreditLog> page = new Page<>(query.getPage(), query.getSize());
        return creditLogMapper.selectPage(page, wrapper).convert(this::toLogVO);
    }

    @Override
    public void applyEvent(String eventType, Long userId, String relatedId) {
        CreditApplyCmd cmd = new CreditApplyCmd();
        cmd.setEventType(eventType);
        cmd.setUserId(userId);
        cmd.setRelatedId(relatedId);
        cmd.setDeltaScore(resolveDelta(eventType));
        cmd.setRemark(resolveRemark(eventType));
        cmd.setIdempotentKey(eventType + ":" + userId + ":" + relatedId);
        apply(cmd);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void apply(CreditApplyCmd cmd) {
        validateApply(cmd);
        if (findLog(cmd.getIdempotentKey()) != null) {
            return;
        }

        RLock lock = redissonClient.getLock("credit:lock:" + cmd.getUserId());
        boolean locked = false;
        try {
            locked = lock.tryLock(5, 30, TimeUnit.SECONDS);
            if (!locked) {
                throw new BizException(99999, "信用分操作繁忙，请稍后重试");
            }
            if (findLog(cmd.getIdempotentKey()) != null) {
                return;
            }

            BizCredit credit = getOrCreateCredit(cmd.getUserId());
            int before = credit.getScore();
            int delta = applyDailyLimitIfNeeded(cmd);
            int after = Math.max(MIN_SCORE, Math.min(MAX_SCORE, before + delta));
            delta = after - before;
            LocalDateTime now = LocalDateTime.now();

            credit.setScore(after);
            credit.setLevelName(resolveLevel(after));
            credit.setStatus(after < DISABLE_THRESHOLD ? 0 : 1);
            credit.setLastEventAt(now);
            credit.setUpdatedAt(now);
            creditMapper.updateById(credit);

            BizCreditLog log = new BizCreditLog();
            log.setId(idWorker.nextId());
            log.setUserId(cmd.getUserId());
            log.setEventType(cmd.getEventType());
            log.setRelatedId(cmd.getRelatedId());
            log.setDeltaScore(delta);
            log.setScoreBefore(before);
            log.setScoreAfter(after);
            log.setRemark(cmd.getRemark());
            log.setIdempotentKey(cmd.getIdempotentKey());
            log.setTenantId(0L);
            log.setCreatedAt(now);
            creditLogMapper.insert(log);

            if (after < DISABLE_THRESHOLD) {
                SysUser user = new SysUser();
                user.setId(cmd.getUserId());
                user.setStatus(0);
                user.setBlacklistReason("信用分低于30，系统自动禁用");
                user.setUpdatedAt(now);
                sysUserMapper.updateById(user);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException(99999, "信用分操作被中断");
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public void adminAdjust(Long userId, Long adminId, CreditAdjustDTO dto) {
        CreditApplyCmd cmd = new CreditApplyCmd();
        cmd.setEventType("ADMIN_ADJUST");
        cmd.setUserId(userId);
        cmd.setRelatedId(String.valueOf(adminId));
        cmd.setDeltaScore(dto.getDeltaScore());
        cmd.setRemark(dto.getRemark());
        cmd.setIdempotentKey(StringUtils.hasText(dto.getIdempotentKey())
                ? dto.getIdempotentKey()
                : "ADMIN_ADJUST:" + userId + ":" + adminId + ":" + System.currentTimeMillis());
        apply(cmd);
    }

    @Override
    public void restoreMonthlyCredit() {
        List<BizCredit> list = creditMapper.selectList(new LambdaQueryWrapper<BizCredit>()
                .lt(BizCredit::getScore, DEFAULT_SCORE));
        String ym = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        for (BizCredit credit : list) {
            CreditApplyCmd cmd = new CreditApplyCmd();
            cmd.setEventType("MONTHLY_RESTORE");
            cmd.setUserId(credit.getUserId());
            cmd.setRelatedId(ym);
            cmd.setDeltaScore(5);
            cmd.setRemark("每月信用分自动恢复");
            cmd.setIdempotentKey("MONTHLY_RESTORE:" + credit.getUserId() + ":" + ym);
            apply(cmd);
        }
    }

    private BizCredit getOrCreateCredit(Long userId) {
        BizCredit credit = creditMapper.selectOne(new LambdaQueryWrapper<BizCredit>()
                .eq(BizCredit::getUserId, userId)
                .last("LIMIT 1"));
        if (credit != null) {
            return credit;
        }
        LocalDateTime now = LocalDateTime.now();
        credit = new BizCredit();
        credit.setId(idWorker.nextId());
        credit.setUserId(userId);
        credit.setScore(DEFAULT_SCORE);
        credit.setLevelName(resolveLevel(DEFAULT_SCORE));
        credit.setStatus(1);
        credit.setTenantId(0L);
        credit.setCreatedAt(now);
        credit.setUpdatedAt(now);
        try {
            creditMapper.insert(credit);
            return credit;
        } catch (Exception e) {
            return creditMapper.selectOne(new LambdaQueryWrapper<BizCredit>()
                    .eq(BizCredit::getUserId, userId)
                    .last("LIMIT 1"));
        }
    }

    private BizCreditLog findLog(String idempotentKey) {
        return creditLogMapper.selectOne(new LambdaQueryWrapper<BizCreditLog>()
                .eq(BizCreditLog::getIdempotentKey, idempotentKey)
                .last("LIMIT 1"));
    }

    private int applyDailyLimitIfNeeded(CreditApplyCmd cmd) {
        int delta = cmd.getDeltaScore();
        if (delta <= 0 || "ADMIN_ADJUST".equals(cmd.getEventType()) || "MONTHLY_RESTORE".equals(cmd.getEventType())) {
            return delta;
        }
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String key = RedisKey.creditDaily(cmd.getUserId(), date);
        String currentVal = redisTemplate.opsForValue().get(key);
        int current = currentVal == null ? 0 : Integer.parseInt(currentVal);
        int remaining = DAILY_POSITIVE_LIMIT - current;
        if (remaining <= 0) {
            return 0;
        }
        int actual = Math.min(delta, remaining);
        redisTemplate.opsForValue().increment(key, actual);
        redisTemplate.expire(key, 2, TimeUnit.DAYS);
        return actual;
    }

    private int resolveDelta(String eventType) {
        return switch (eventType) {
            case "ORDER_DONE" -> 3;
            case "REVIEW_POSTED" -> 2;
            case "ORDER_TIMEOUT" -> -5;
            case "BID_REVOKED" -> -10;
            default -> throw new BizException(10001, "不支持的信用事件：" + eventType);
        };
    }

    private String resolveRemark(String eventType) {
        return switch (eventType) {
            case "ORDER_DONE" -> "订单完成加分";
            case "REVIEW_POSTED" -> "发布评价加分";
            case "ORDER_TIMEOUT" -> "订单超时扣分";
            case "BID_REVOKED" -> "出价撤销扣分";
            default -> eventType;
        };
    }

    private String resolveLevel(int score) {
        if (score >= 90) {
            return "优秀";
        }
        if (score >= 80) {
            return "良好";
        }
        if (score >= 60) {
            return "一般";
        }
        if (score >= 30) {
            return "风险";
        }
        return "禁用";
    }

    private void validateApply(CreditApplyCmd cmd) {
        if (cmd == null || cmd.getUserId() == null || !StringUtils.hasText(cmd.getEventType())
                || !StringUtils.hasText(cmd.getRelatedId()) || cmd.getDeltaScore() == null
                || !StringUtils.hasText(cmd.getIdempotentKey())) {
            throw new BizException(10001, "信用分参数不完整");
        }
    }

    private CreditVO toVO(BizCredit credit) {
        CreditVO vo = new CreditVO();
        vo.setUserId(credit.getUserId());
        vo.setScore(credit.getScore());
        vo.setLevelName(credit.getLevelName());
        vo.setStatus(credit.getStatus());
        vo.setLastEventAt(credit.getLastEventAt());
        vo.setUpdatedAt(credit.getUpdatedAt());
        return vo;
    }

    private CreditLogVO toLogVO(BizCreditLog log) {
        CreditLogVO vo = new CreditLogVO();
        vo.setId(log.getId());
        vo.setUserId(log.getUserId());
        vo.setEventType(log.getEventType());
        vo.setRelatedId(log.getRelatedId());
        vo.setDeltaScore(log.getDeltaScore());
        vo.setScoreBefore(log.getScoreBefore());
        vo.setScoreAfter(log.getScoreAfter());
        vo.setRemark(log.getRemark());
        vo.setCreatedAt(log.getCreatedAt());
        return vo;
    }
}
