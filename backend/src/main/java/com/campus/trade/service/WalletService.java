package com.campus.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.trade.common.BizException;
import com.campus.trade.entity.User;
import com.campus.trade.entity.WalletTxn;
import com.campus.trade.mapper.UserMapper;
import com.campus.trade.mapper.WalletTxnMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class WalletService {

    private final UserMapper userMapper;
    private final WalletTxnMapper walletTxnMapper;

    public WalletService(UserMapper userMapper, WalletTxnMapper walletTxnMapper) {
        this.userMapper = userMapper;
        this.walletTxnMapper = walletTxnMapper;
    }

    public Map<String, Object> info(Long userId) {
        User user = userMapper.selectById(userId);
        Map<String, Object> data = new HashMap<>();
        data.put("balance", user.getBalance());
        data.put("points", user.getPoints());
        data.put("creditScore", user.getCreditScore());
        return data;
    }

    public List<WalletTxn> txns(Long userId) {
        return walletTxnMapper.selectList(new LambdaQueryWrapper<WalletTxn>()
                .eq(WalletTxn::getUserId, userId)
                .orderByDesc(WalletTxn::getCreatedAt)
                .last("limit 100"));
    }

    /**
     * 管理员给指定用户充值
     */
    @Transactional(rollbackFor = Exception.class)
    public void recharge(Long userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("充值金额必须大于 0");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        user.setBalance(user.getBalance().add(amount));
        userMapper.updateById(user);

        WalletTxn txn = new WalletTxn();
        txn.setUserId(userId);
        txn.setType("RECHARGE");
        txn.setAmount(amount);
        txn.setBalanceAfter(user.getBalance());
        txn.setRemark("管理员充值");
        txn.setCreatedAt(LocalDateTime.now());
        walletTxnMapper.insert(txn);
    }
}
