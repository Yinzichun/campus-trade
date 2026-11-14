package com.campus.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.trade.common.BizException;
import com.campus.trade.entity.Order;
import com.campus.trade.entity.Product;
import com.campus.trade.entity.User;
import com.campus.trade.mapper.OrderMapper;
import com.campus.trade.mapper.ProductMapper;
import com.campus.trade.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {

    private final UserMapper userMapper;
    private final ProductMapper productMapper;
    private final OrderMapper orderMapper;
    private final WalletService walletService;

    public AdminService(UserMapper userMapper, ProductMapper productMapper,
                        OrderMapper orderMapper, WalletService walletService) {
        this.userMapper = userMapper;
        this.productMapper = productMapper;
        this.orderMapper = orderMapper;
        this.walletService = walletService;
    }

    public List<User> users(String role, String status, String keyword) {
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(role)) {
            qw.eq(User::getRole, role);
        }
        if (StringUtils.hasText(status)) {
            qw.eq(User::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(User::getUsername, keyword)
                    .or().like(User::getName, keyword)
                    .or().like(User::getShopName, keyword));
        }
        qw.orderByDesc(User::getCreatedAt);
        List<User> list = userMapper.selectList(qw);
        list.forEach(u -> u.setPassword(null));
        return list;
    }

    public void auditUser(Long id, boolean pass) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        if (!"PENDING".equals(user.getStatus())) {
            throw new BizException("该用户不在待审核状态");
        }
        user.setStatus(pass ? "ACTIVE" : "REJECTED");
        userMapper.updateById(user);
    }

    public void updateUser(User form) {
        User user = userMapper.selectById(form.getId());
        if (user == null) {
            throw new BizException("用户不存在");
        }
        if (StringUtils.hasText(form.getName())) {
            user.setName(form.getName());
        }
        user.setPhone(form.getPhone());
        user.setEmail(form.getEmail());
        user.setCity(form.getCity());
        if (form.getGender() != null) {
            user.setGender(form.getGender());
        }
        if (StringUtils.hasText(form.getShopName())) {
            user.setShopName(form.getShopName());
        }
        if (form.getMerchantLevel() != null && "MERCHANT".equals(user.getRole())) {
            user.setMerchantLevel(Math.max(1, Math.min(5, form.getMerchantLevel())));
        }
        userMapper.updateById(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        if ("ADMIN".equals(user.getRole())) {
            throw new BizException("管理员账号不能删除");
        }
        userMapper.deleteById(id);
    }

    public void recharge(Long userId, BigDecimal amount) {
        walletService.recharge(userId, amount);
    }

    public void setLevel(Long userId, Integer level) {
        User user = userMapper.selectById(userId);
        if (user == null || !"MERCHANT".equals(user.getRole())) {
            throw new BizException("该用户不是商家");
        }
        user.setMerchantLevel(Math.max(1, Math.min(5, level)));
        userMapper.updateById(user);
    }

    /**
     * 限时封禁：封禁期内不能发布商品、不能下单
     */
    public void ban(Long userId, int days) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        if (days <= 0) {
            user.setStatus("ACTIVE");
            user.setBannedUntil(null);
        } else {
            user.setStatus("BANNED");
            user.setBannedUntil(LocalDateTime.now().plusDays(days));
        }
        userMapper.updateById(user);
    }

    public List<Product> products(String status) {
        LambdaQueryWrapper<Product> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            qw.eq(Product::getStatus, status);
        }
        qw.orderByDesc(Product::getCreatedAt);
        List<Product> list = productMapper.selectList(qw);
        for (Product p : list) {
            User m = userMapper.selectById(p.getMerchantId());
            if (m != null) {
                p.setShopName(m.getShopName());
                p.setMerchantName(m.getName());
            }
            p.setImageList(StringUtils.hasText(p.getImages())
                    ? java.util.Arrays.asList(p.getImages().split(",")) : java.util.List.of());
        }
        return list;
    }

    public Map<String, Object> stats() {
        Map<String, Object> data = new HashMap<>();
        data.put("userCount", userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getRole, "USER")));
        data.put("merchantCount", userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getRole, "MERCHANT")));
        data.put("pendingUsers", userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getStatus, "PENDING")));
        data.put("pendingProducts", productMapper.selectCount(new LambdaQueryWrapper<Product>().eq(Product::getStatus, "PENDING")));
        data.put("onSaleProducts", productMapper.selectCount(new LambdaQueryWrapper<Product>().eq(Product::getStatus, "ON_SALE")));
        data.put("orderCount", orderMapper.selectCount(null));
        List<Order> orders = orderMapper.selectList(null);
        BigDecimal gmv = orders.stream()
                .filter(o -> !"UNPAID".equals(o.getStatus()) && !"CANCELLED".equals(o.getStatus()))
                .map(Order::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal fee = orders.stream()
                .filter(o -> "COMPLETED".equals(o.getStatus()))
                .map(Order::getFee).reduce(BigDecimal.ZERO, BigDecimal::add);
        data.put("gmv", gmv);
        data.put("feeIncome", fee);
        return data;
    }
}
