package com.auction.business.service.impl;

import com.auction.business.dto.WalletAdjustCmd;
import com.auction.business.dto.WalletAdjustDTO;
import com.auction.business.dto.WalletTransactionQueryDTO;
import com.auction.business.entity.BizWallet;
import com.auction.business.entity.BizWalletTransaction;
import com.auction.business.mapper.BizWalletMapper;
import com.auction.business.mapper.BizWalletTransactionMapper;
import com.auction.business.service.WalletService;
import com.auction.business.vo.WalletSummaryVO;
import com.auction.business.vo.WalletTransactionVO;
import com.auction.business.vo.WalletVO;
import com.auction.common.core.ErrorCode;
import com.auction.common.exception.BizException;
import com.auction.common.util.SnowflakeIdWorker;
import com.auction.system.entity.SysUser;
import com.auction.system.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 钱包服务实现。
 * 核心原则：钱包主表负责快速查询余额，流水表负责审计和对账，两张表必须在同一事务内一起更新。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal MAX_ADMIN_ADJUST_AMOUNT = new BigDecimal("100000.00");
    private static final DateTimeFormatter TX_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final BizWalletMapper walletMapper;
    private final BizWalletTransactionMapper transactionMapper;
    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final RedissonClient redissonClient;
    private final SnowflakeIdWorker idWorker = new SnowflakeIdWorker();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BizWalletTransaction adjust(WalletAdjustCmd cmd) {
        validateAdjustCmd(cmd);
        BizWalletTransaction exists = findByIdempotentKey(cmd.getIdempotentKey());
        if (exists != null) {
            return exists;
        }

        RLock lock = redissonClient.getLock("wallet:lock:" + cmd.getUserId());
        boolean locked = false;
        try {
            locked = lock.tryLock(5, 30, TimeUnit.SECONDS);
            if (!locked) {
                throw new BizException(99999, "钱包操作繁忙，请稍后重试");
            }
            exists = findByIdempotentKey(cmd.getIdempotentKey());
            if (exists != null) {
                return exists;
            }

            BizWallet wallet = getOrCreateWallet(cmd.getUserId());
            if (!Integer.valueOf(1).equals(wallet.getStatus())) {
                throw new BizException(50006, "钱包状态异常，暂不可操作");
            }

            BigDecimal balanceBefore = nvl(wallet.getBalance());
            BigDecimal frozenBefore = nvl(wallet.getFrozenBalance());
            BigDecimal balanceAfter = balanceBefore;
            BigDecimal frozenAfter = frozenBefore;
            String action = normalizeAction(cmd.getActionType());
            int direction = 0;

            switch (action) {
                case "RECHARGE" -> {
                    balanceAfter = balanceBefore.add(cmd.getAmount());
                    direction = 1;
                }
                case "DEDUCT" -> {
                    ensureEnough(balanceBefore, cmd.getAmount(), ErrorCode.WALLET_BALANCE_NOT_ENOUGH.getCode(), "余额不足");
                    balanceAfter = balanceBefore.subtract(cmd.getAmount());
                    direction = -1;
                }
                case "FREEZE", "BID_FREEZE" -> {
                    ensureEnough(balanceBefore, cmd.getAmount(), ErrorCode.WALLET_BALANCE_NOT_ENOUGH.getCode(), "余额不足，无法冻结保证金");
                    balanceAfter = balanceBefore.subtract(cmd.getAmount());
                    frozenAfter = frozenBefore.add(cmd.getAmount());
                }
                case "UNFREEZE", "BID_UNFREEZE" -> {
                    ensureEnough(frozenBefore, cmd.getAmount(), 50007, "冻结金额不足，无法解冻");
                    balanceAfter = balanceBefore.add(cmd.getAmount());
                    frozenAfter = frozenBefore.subtract(cmd.getAmount());
                }
                case "BID_DEDUCT" -> {
                    ensureEnough(frozenBefore, cmd.getAmount(), 50007, "冻结金额不足，无法抵扣");
                    frozenAfter = frozenBefore.subtract(cmd.getAmount());
                    direction = -1;
                }
                default -> throw new BizException(10001, "不支持的钱包动作：" + action);
            }

            LocalDateTime now = LocalDateTime.now();
            wallet.setBalance(balanceAfter);
            wallet.setFrozenBalance(frozenAfter);
            wallet.setUpdatedAt(now);
            walletMapper.updateById(wallet);

            BizWalletTransaction tx = buildTransaction(cmd, wallet, action, direction,
                    balanceBefore, balanceAfter, frozenBefore, frozenAfter, now);
            transactionMapper.insert(tx);
            log.info("钱包流水创建成功: userId={}, action={}, amount={}, txNo={}",
                    cmd.getUserId(), action, cmd.getAmount(), tx.getTransactionNo());
            return tx;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException(99999, "钱包操作被中断");
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public WalletVO getWallet(Long userId) {
        return toWalletVO(getOrCreateWallet(userId));
    }

    @Override
    public IPage<WalletTransactionVO> listMyTransactions(Long userId, WalletTransactionQueryDTO query) {
        query.setUserId(userId);
        return listAllTransactions(query);
    }

    @Override
    public IPage<WalletTransactionVO> listAllTransactions(WalletTransactionQueryDTO query) {
        int page = query.getPage() == null || query.getPage() <= 0 ? 1 : query.getPage();
        int size = query.getSize() == null || query.getSize() <= 0 ? 20 : query.getSize();
        Page<BizWalletTransaction> p = new Page<>(page, size);
        LambdaQueryWrapper<BizWalletTransaction> wrapper = new LambdaQueryWrapper<BizWalletTransaction>()
                .eq(query.getUserId() != null, BizWalletTransaction::getUserId, query.getUserId())
                .eq(StringUtils.hasText(query.getActionType()), BizWalletTransaction::getActionType, normalizeAction(query.getActionType()))
                .eq(StringUtils.hasText(query.getBizType()), BizWalletTransaction::getBizType, query.getBizType())
                .orderByDesc(BizWalletTransaction::getCreatedAt);
        return transactionMapper.selectPage(p, wrapper).convert(this::toTransactionVO);
    }

    @Override
    public WalletSummaryVO getSummary() {
        List<BizWallet> wallets = walletMapper.selectList(null);
        BigDecimal totalBalance = wallets.stream().map(w -> nvl(w.getBalance())).reduce(ZERO, BigDecimal::add);
        BigDecimal totalFrozen = wallets.stream().map(w -> nvl(w.getFrozenBalance())).reduce(ZERO, BigDecimal::add);
        WalletSummaryVO vo = new WalletSummaryVO();
        vo.setWalletCount((long) wallets.size());
        vo.setTotalBalance(totalBalance);
        vo.setTotalFrozenBalance(totalFrozen);
        vo.setTotalAmount(totalBalance.add(totalFrozen));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminAdjust(Long targetUserId, Long adminId, WalletAdjustDTO dto) {
        if (Objects.equals(targetUserId, adminId)) {
            throw new BizException(10003, "管理员不能给自己调账");
        }
        if (dto.getAmount().compareTo(MAX_ADMIN_ADJUST_AMOUNT) > 0) {
            throw new BizException(10001, "单次调账金额不能超过 " + MAX_ADMIN_ADJUST_AMOUNT.toPlainString());
        }
        String action = normalizeAction(dto.getActionType());
        if (!List.of("RECHARGE", "DEDUCT", "FREEZE", "UNFREEZE").contains(action)) {
            throw new BizException(10001, "管理端只允许 RECHARGE/DEDUCT/FREEZE/UNFREEZE");
        }

        SysUser admin = sysUserMapper.selectById(adminId);
        if (admin == null || !passwordEncoder.matches(dto.getAdminPassword(), admin.getPassword())) {
            throw new BizException(ErrorCode.PASSWORD_ERROR);
        }
        SysUser target = sysUserMapper.selectById(targetUserId);
        if (target == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }

        WalletAdjustCmd cmd = new WalletAdjustCmd();
        cmd.setUserId(targetUserId);
        cmd.setActionType(action);
        cmd.setAmount(dto.getAmount());
        cmd.setBizType("ADMIN_ADJUST");
        cmd.setBizId(String.valueOf(targetUserId));
        cmd.setOperatorId(adminId);
        cmd.setRemark(dto.getRemark());
        cmd.setIdempotentKey(StringUtils.hasText(dto.getIdempotentKey())
                ? dto.getIdempotentKey()
                : "ADMIN_ADJUST:" + idWorker.nextId());
        adjust(cmd);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean freezeBidDeposit(Long userId, Long itemId, BigDecimal amount, String requestId) {
        if (amount == null || amount.compareTo(ZERO) <= 0) {
            return false;
        }
        RLock lock = redissonClient.getLock("wallet:lock:" + userId);
        boolean locked = false;
        try {
            locked = lock.tryLock(5, 30, TimeUnit.SECONDS);
            if (!locked) {
                throw new BizException(99999, "钱包操作繁忙，请稍后重试");
            }
            if (getActiveBidDeposit(userId, itemId).compareTo(ZERO) > 0) {
                return false;
            }
            WalletAdjustCmd cmd = bidCmd(userId, itemId, amount, "BID_FREEZE",
                    "BID_FREEZE:" + itemId + ":" + userId + ":" + requestId,
                    "首次出价冻结保证金");
            adjust(cmd);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException(99999, "钱包操作被中断");
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelBidDepositFreeze(Long userId, Long itemId, BigDecimal amount, String requestId) {
        BigDecimal active = getActiveBidDeposit(userId, itemId);
        if (active.compareTo(ZERO) <= 0) {
            return;
        }
        WalletAdjustCmd cmd = bidCmd(userId, itemId, active.min(amount), "BID_UNFREEZE",
                "BID_UNFREEZE_CANCEL:" + itemId + ":" + userId + ":" + requestId,
                "出价未成功，退回本次冻结保证金");
        adjust(cmd);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void settleBidDeposits(Long itemId, Long winnerId, BigDecimal depositAmount) {
        if (depositAmount == null || depositAmount.compareTo(ZERO) <= 0) {
            return;
        }
        List<Long> userIds = transactionMapper.selectList(new LambdaQueryWrapper<BizWalletTransaction>()
                        .eq(BizWalletTransaction::getRelatedItemId, itemId)
                        .eq(BizWalletTransaction::getActionType, "BID_FREEZE"))
                .stream()
                .map(BizWalletTransaction::getUserId)
                .distinct()
                .toList();

        for (Long userId : userIds) {
            BigDecimal active = getActiveBidDeposit(userId, itemId);
            if (active.compareTo(ZERO) <= 0) {
                continue;
            }
            if (Objects.equals(userId, winnerId)) {
                adjust(bidCmd(userId, itemId, active.min(depositAmount), "BID_DEDUCT",
                        "BID_DEDUCT:" + itemId + ":" + userId,
                        "中标后保证金抵扣订单金额"));
            } else {
                adjust(bidCmd(userId, itemId, active, "BID_UNFREEZE",
                        "BID_UNFREEZE_SETTLE:" + itemId + ":" + userId,
                        "拍卖结算未中标，解冻保证金"));
            }
        }
    }

    private BizWallet getOrCreateWallet(Long userId) {
        BizWallet wallet = walletMapper.selectOne(new LambdaQueryWrapper<BizWallet>()
                .eq(BizWallet::getUserId, userId)
                .last("LIMIT 1"));
        if (wallet != null) {
            return wallet;
        }
        LocalDateTime now = LocalDateTime.now();
        wallet = new BizWallet();
        wallet.setId(idWorker.nextId());
        wallet.setUserId(userId);
        wallet.setBalance(ZERO);
        wallet.setFrozenBalance(ZERO);
        wallet.setStatus(1);
        wallet.setTenantId(0L);
        wallet.setCreatedAt(now);
        wallet.setUpdatedAt(now);
        wallet.setVersion(0);
        walletMapper.insert(wallet);
        return wallet;
    }

    private BigDecimal getActiveBidDeposit(Long userId, Long itemId) {
        List<BizWalletTransaction> txList = transactionMapper.selectList(new LambdaQueryWrapper<BizWalletTransaction>()
                .eq(BizWalletTransaction::getUserId, userId)
                .eq(BizWalletTransaction::getRelatedItemId, itemId)
                .eq(BizWalletTransaction::getBizType, "BID_DEPOSIT"));
        BigDecimal active = ZERO;
        for (BizWalletTransaction tx : txList) {
            if ("BID_FREEZE".equals(tx.getActionType())) {
                active = active.add(tx.getAmount());
            } else if ("BID_UNFREEZE".equals(tx.getActionType()) || "BID_DEDUCT".equals(tx.getActionType())) {
                active = active.subtract(tx.getAmount());
            }
        }
        return active.max(ZERO);
    }

    private WalletAdjustCmd bidCmd(Long userId, Long itemId, BigDecimal amount, String action,
                                   String idempotentKey, String remark) {
        WalletAdjustCmd cmd = new WalletAdjustCmd();
        cmd.setUserId(userId);
        cmd.setActionType(action);
        cmd.setAmount(amount);
        cmd.setBizType("BID_DEPOSIT");
        cmd.setBizId(String.valueOf(itemId));
        cmd.setRelatedItemId(itemId);
        cmd.setOperatorId(userId);
        cmd.setRemark(remark);
        cmd.setIdempotentKey(idempotentKey);
        return cmd;
    }

    private BizWalletTransaction buildTransaction(WalletAdjustCmd cmd, BizWallet wallet, String action, int direction,
                                                  BigDecimal balanceBefore, BigDecimal balanceAfter,
                                                  BigDecimal frozenBefore, BigDecimal frozenAfter,
                                                  LocalDateTime now) {
        long txId = idWorker.nextId();
        BizWalletTransaction tx = new BizWalletTransaction();
        tx.setId(txId);
        tx.setTransactionNo("WT" + now.format(TX_TIME) + txId);
        tx.setWalletId(wallet.getId());
        tx.setUserId(cmd.getUserId());
        tx.setActionType(action);
        tx.setDirection(direction);
        tx.setAmount(cmd.getAmount());
        tx.setBalanceBefore(balanceBefore);
        tx.setBalanceAfter(balanceAfter);
        tx.setFrozenBefore(frozenBefore);
        tx.setFrozenAfter(frozenAfter);
        tx.setBizType(cmd.getBizType());
        tx.setBizId(cmd.getBizId());
        tx.setRelatedItemId(cmd.getRelatedItemId());
        tx.setOperatorId(cmd.getOperatorId());
        tx.setRemark(cmd.getRemark());
        tx.setIdempotentKey(cmd.getIdempotentKey());
        tx.setTenantId(0L);
        tx.setCreatedAt(now);
        return tx;
    }

    private BizWalletTransaction findByIdempotentKey(String idempotentKey) {
        if (!StringUtils.hasText(idempotentKey)) {
            return null;
        }
        return transactionMapper.selectOne(new LambdaQueryWrapper<BizWalletTransaction>()
                .eq(BizWalletTransaction::getIdempotentKey, idempotentKey)
                .last("LIMIT 1"));
    }

    private void validateAdjustCmd(WalletAdjustCmd cmd) {
        if (cmd == null || cmd.getUserId() == null || cmd.getAmount() == null
                || cmd.getAmount().compareTo(ZERO) <= 0 || !StringUtils.hasText(cmd.getActionType())) {
            throw new BizException(ErrorCode.PARAM_ERROR);
        }
    }

    private String normalizeAction(String action) {
        return action == null ? "" : action.trim().toUpperCase(Locale.ROOT);
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    private void ensureEnough(BigDecimal current, BigDecimal amount, Integer code, String message) {
        if (current.compareTo(amount) < 0) {
            throw new BizException(code, message);
        }
    }

    private WalletVO toWalletVO(BizWallet wallet) {
        WalletVO vo = new WalletVO();
        vo.setId(wallet.getId());
        vo.setUserId(wallet.getUserId());
        vo.setBalance(nvl(wallet.getBalance()));
        vo.setFrozenBalance(nvl(wallet.getFrozenBalance()));
        vo.setTotalAmount(nvl(wallet.getBalance()).add(nvl(wallet.getFrozenBalance())));
        vo.setStatus(wallet.getStatus());
        vo.setUpdatedAt(wallet.getUpdatedAt());
        return vo;
    }

    private WalletTransactionVO toTransactionVO(BizWalletTransaction tx) {
        WalletTransactionVO vo = new WalletTransactionVO();
        vo.setId(tx.getId());
        vo.setTransactionNo(tx.getTransactionNo());
        vo.setUserId(tx.getUserId());
        vo.setActionType(tx.getActionType());
        vo.setDirection(tx.getDirection());
        vo.setAmount(tx.getAmount());
        vo.setBalanceBefore(tx.getBalanceBefore());
        vo.setBalanceAfter(tx.getBalanceAfter());
        vo.setFrozenBefore(tx.getFrozenBefore());
        vo.setFrozenAfter(tx.getFrozenAfter());
        vo.setBizType(tx.getBizType());
        vo.setBizId(tx.getBizId());
        vo.setRelatedItemId(tx.getRelatedItemId());
        vo.setOperatorId(tx.getOperatorId());
        vo.setRemark(tx.getRemark());
        vo.setCreatedAt(tx.getCreatedAt());
        return vo;
    }
}
