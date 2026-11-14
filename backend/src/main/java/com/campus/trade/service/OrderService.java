package com.campus.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.trade.common.BizException;
import com.campus.trade.config.AppProps;
import com.campus.trade.entity.Blacklist;
import com.campus.trade.entity.CartItem;
import com.campus.trade.entity.Order;
import com.campus.trade.entity.Product;
import com.campus.trade.entity.Review;
import com.campus.trade.entity.User;
import com.campus.trade.entity.WalletTxn;
import com.campus.trade.mapper.BlacklistMapper;
import com.campus.trade.mapper.CartItemMapper;
import com.campus.trade.mapper.OrderMapper;
import com.campus.trade.mapper.ProductMapper;
import com.campus.trade.mapper.ReviewMapper;
import com.campus.trade.mapper.UserMapper;
import com.campus.trade.mapper.WalletTxnMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 订单与资金核心业务：
 * 下单即扣款（模拟支付） -> 货款进入平台中间账户 -> 买家确认收货（或7天自动确认）-> 扣除平台手续费后打给商家
 */
@Service
public class OrderService {

    private final OrderMapper orderMapper;
    private final ProductMapper productMapper;
    private final UserMapper userMapper;
    private final CartItemMapper cartItemMapper;
    private final WalletTxnMapper walletTxnMapper;
    private final ReviewMapper reviewMapper;
    private final BlacklistMapper blacklistMapper;
    private final AppProps appProps;

    public OrderService(OrderMapper orderMapper, ProductMapper productMapper, UserMapper userMapper,
                        CartItemMapper cartItemMapper, WalletTxnMapper walletTxnMapper,
                        ReviewMapper reviewMapper, BlacklistMapper blacklistMapper, AppProps appProps) {
        this.orderMapper = orderMapper;
        this.productMapper = productMapper;
        this.userMapper = userMapper;
        this.cartItemMapper = cartItemMapper;
        this.walletTxnMapper = walletTxnMapper;
        this.reviewMapper = reviewMapper;
        this.blacklistMapper = blacklistMapper;
        this.appProps = appProps;
    }

    /**
     * 结算下单：购物车多商品一次提交，按商品拆分订单，余额不足直接拒绝
     */
    @Transactional(rollbackFor = Exception.class)
    public List<Order> checkout(Long buyerId, List<Long> cartItemIds, Long productId,
                                Integer quantity, Integer usePoints) {
        User buyer = userMapper.selectById(buyerId);
        if (buyer == null) {
            throw new BizException("用户不存在");
        }
        if ("BANNED".equals(buyer.getStatus())) {
            throw new BizException("账号已被封禁，无法下单");
        }

        List<Product> products = new ArrayList<>();
        List<Integer> quantities = new ArrayList<>();
        List<Long> usedCartIds = new ArrayList<>();

        if (productId != null) {
            Product p = productMapper.selectById(productId);
            if (p == null) {
                throw new BizException("商品不存在");
            }
            products.add(p);
            quantities.add(quantity == null || quantity < 1 ? 1 : quantity);
        } else {
            if (cartItemIds == null || cartItemIds.isEmpty()) {
                throw new BizException("请选择要结算的商品");
            }
            for (Long cid : cartItemIds) {
                CartItem item = cartItemMapper.selectById(cid);
                if (item == null || !item.getUserId().equals(buyerId)) {
                    throw new BizException("购物车记录不存在");
                }
                Product p = productMapper.selectById(item.getProductId());
                if (p == null) {
                    throw new BizException("商品已被删除");
                }
                products.add(p);
                quantities.add(item.getQuantity());
                usedCartIds.add(cid);
            }
        }

        // 校验：在售、库存、黑名单
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < products.size(); i++) {
            Product p = products.get(i);
            if (!"ON_SALE".equals(p.getStatus())) {
                throw new BizException("商品【" + p.getName() + "】已下架或已售出");
            }
            if (p.getStock() < quantities.get(i)) {
                throw new BizException("商品【" + p.getName() + "】库存不足");
            }
            Long black = blacklistMapper.selectCount(new LambdaQueryWrapper<Blacklist>()
                    .eq(Blacklist::getMerchantId, p.getMerchantId())
                    .eq(Blacklist::getBuyerId, buyerId));
            if (black != null && black > 0) {
                throw new BizException("你已被店铺【" + p.getShopName() + "】拉黑，无法购买");
            }
            total = total.add(p.getPrice().multiply(new BigDecimal(quantities.get(i))));
        }
        total = total.setScale(2, RoundingMode.HALF_UP);

        // 积分抵扣：100 积分 = 1 元，不能超过应付金额
        int usable = Math.min(buyer.getPoints() == null ? 0 : buyer.getPoints(),
                total.multiply(new BigDecimal(100)).intValue());
        int pointsUsed = usePoints == null ? 0 : Math.min(usePoints, usable);
        if (pointsUsed < 0) {
            pointsUsed = 0;
        }
        BigDecimal pointsDeduct = new BigDecimal(pointsUsed)
                .divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);
        BigDecimal payable = total.subtract(pointsDeduct).setScale(2, RoundingMode.HALF_UP);
        if (payable.compareTo(BigDecimal.ZERO) <= 0) {
            payable = new BigDecimal("0.01");
        }
        if (buyer.getBalance().compareTo(payable) < 0) {
            throw new BizException("钱包余额不足，请先充值（应付 " + payable + " 元）");
        }

        // 积分抵扣金额按订单金额比例分摊
        BigDecimal allocated = BigDecimal.ZERO;
        List<Order> orders = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < products.size(); i++) {
            Product p = products.get(i);
            int qty = quantities.get(i);
            BigDecimal origin = p.getPrice().multiply(new BigDecimal(qty)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal deduct;
            if (i == products.size() - 1) {
                deduct = pointsDeduct.subtract(allocated);
            } else {
                deduct = pointsDeduct.multiply(origin).divide(total, 2, RoundingMode.HALF_UP);
                allocated = allocated.add(deduct);
            }
            BigDecimal amount = origin.subtract(deduct).setScale(2, RoundingMode.HALF_UP);

            User merchant = userMapper.selectById(p.getMerchantId());
            BigDecimal rate = appProps.feeRate(merchant.getMerchantLevel());
            BigDecimal fee = amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);

            Order order = new Order();
            order.setOrderNo(generateNo());
            order.setBuyerId(buyerId);
            order.setMerchantId(p.getMerchantId());
            order.setProductId(p.getId());
            order.setProductName(p.getName());
            order.setProductImage(p.getImageList() != null && !p.getImageList().isEmpty() ? p.getImageList().get(0) : null);
            order.setQuantity(qty);
            order.setOriginAmount(origin);
            order.setAmount(amount);
            order.setFee(fee);
            order.setPointsUsed(i == 0 ? pointsUsed : 0);
            order.setPointsDeduct(i == 0 ? pointsDeduct : BigDecimal.ZERO);
            order.setStatus("PAID");
            order.setCreatedAt(now);
            order.setPayTime(now);
            order.setConfirmDeadline(now.plusDays(appProps.getAutoConfirmDays()));
            orderMapper.insert(order);
            orders.add(order);

            // 扣库存、加销量
            p.setStock(p.getStock() - qty);
            p.setSales((p.getSales() == null ? 0 : p.getSales()) + qty);
            if (p.getStock() <= 0) {
                p.setStatus("SOLD");
            }
            productMapper.updateById(p);
        }

        // 扣买家余额与积分，同时按消费额累加积分（1元=1积分）
        buyer.setBalance(buyer.getBalance().subtract(payable));
        buyer.setPoints(buyer.getPoints() - pointsUsed + payable.intValue());
        userMapper.updateById(buyer);
        addTxn(buyerId, "PAY", payable.negate(), buyer.getBalance(),
                "订单支付（" + orders.size() + " 笔）", orders.get(0).getId());

        // 清空已结算的购物车记录
        for (Long cid : usedCartIds) {
            cartItemMapper.deleteById(cid);
        }
        return orders;
    }

    /**
     * 买家确认收货，货款结算给商家
     */
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long buyerId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getBuyerId().equals(buyerId)) {
            throw new BizException("订单不存在");
        }
        if (!"PAID".equals(order.getStatus())) {
            throw new BizException("当前订单状态不能确认收货");
        }
        settle(order);
    }

    /**
     * 结算：平台扣手续费后把货款打给商家
     */
    @Transactional(rollbackFor = Exception.class)
    public void settle(Order order) {
        LocalDateTime now = LocalDateTime.now();
        User merchant = userMapper.selectById(order.getMerchantId());
        BigDecimal income = order.getAmount().subtract(order.getFee());
        merchant.setBalance(merchant.getBalance().add(income));
        userMapper.updateById(merchant);
        addTxn(merchant.getId(), "INCOME", income, merchant.getBalance(),
                "订单 " + order.getOrderNo() + " 货款入账（已扣平台服务费 " + order.getFee() + " 元）", order.getId());

        order.setStatus("COMPLETED");
        order.setReceiveTime(now);
        order.setReturnDeadline(now.plusHours(appProps.getReturnHours()));
        order.setSettleTime(now);
        orderMapper.updateById(order);
    }

    /**
     * 买家申请退货（收货后 24 小时内）
     */
    public void requestReturn(Long buyerId, Long orderId, String reason) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getBuyerId().equals(buyerId)) {
            throw new BizException("订单不存在");
        }
        if (!"COMPLETED".equals(order.getStatus())) {
            throw new BizException("只有已完成的订单可以申请退货");
        }
        if (order.getReturnDeadline() == null || LocalDateTime.now().isAfter(order.getReturnDeadline())) {
            throw new BizException("已超过收货后 24 小时，无法申请退货");
        }
        order.setStatus("RETURN_REQUESTED");
        order.setReturnReason(reason);
        orderMapper.updateById(order);
    }

    /**
     * 商家处理退货：同意则退款并恢复库存，拒绝则回到已完成
     */
    @Transactional(rollbackFor = Exception.class)
    public void handleReturn(Long merchantId, Long orderId, boolean approve) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getMerchantId().equals(merchantId)) {
            throw new BizException("订单不存在");
        }
        if (!"RETURN_REQUESTED".equals(order.getStatus())) {
            throw new BizException("该订单没有待处理的退货申请");
        }
        if (!approve) {
            order.setStatus("COMPLETED");
            orderMapper.updateById(order);
            return;
        }
        User buyer = userMapper.selectById(order.getBuyerId());
        buyer.setBalance(buyer.getBalance().add(order.getAmount()));
        buyer.setPoints(Math.max(0, buyer.getPoints() - order.getAmount().intValue()));
        userMapper.updateById(buyer);
        addTxn(buyer.getId(), "REFUND", order.getAmount(), buyer.getBalance(),
                "订单 " + order.getOrderNo() + " 退货退款", order.getId());

        User merchant = userMapper.selectById(order.getMerchantId());
        BigDecimal income = order.getAmount().subtract(order.getFee());
        merchant.setBalance(merchant.getBalance().subtract(income));
        userMapper.updateById(merchant);
        addTxn(merchant.getId(), "REFUND", income.negate(), merchant.getBalance(),
                "订单 " + order.getOrderNo() + " 退货扣回货款", order.getId());

        Product product = productMapper.selectById(order.getProductId());
        if (product != null) {
            product.setStock(product.getStock() + order.getQuantity());
            product.setSales(Math.max(0, product.getSales() - order.getQuantity()));
            if ("SOLD".equals(product.getStatus())) {
                product.setStatus("ON_SALE");
            }
            productMapper.updateById(product);
        }
        order.setStatus("RETURNED");
        orderMapper.updateById(order);
    }

    /**
     * 7 天未确认自动收货（定时任务调用）
     */
    public int autoConfirm() {
        List<Order> list = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, "PAID")
                .le(Order::getConfirmDeadline, LocalDateTime.now()));
        for (Order order : list) {
            settle(order);
        }
        return list.size();
    }

    public List<Order> buyerOrders(Long buyerId, String status) {
        LambdaQueryWrapper<Order> qw = new LambdaQueryWrapper<Order>()
                .eq(Order::getBuyerId, buyerId)
                .orderByDesc(Order::getCreatedAt);
        if (status != null && !status.isEmpty()) {
            qw.eq(Order::getStatus, status);
        }
        List<Order> list = orderMapper.selectList(qw);
        enrich(list);
        return list;
    }

    public List<Order> merchantOrders(Long merchantId, String status) {
        LambdaQueryWrapper<Order> qw = new LambdaQueryWrapper<Order>()
                .eq(Order::getMerchantId, merchantId)
                .orderByDesc(Order::getCreatedAt);
        if (status != null && !status.isEmpty()) {
            qw.eq(Order::getStatus, status);
        }
        List<Order> list = orderMapper.selectList(qw);
        enrich(list);
        return list;
    }

    public void enrich(List<Order> list) {
        for (Order o : list) {
            User merchant = userMapper.selectById(o.getMerchantId());
            if (merchant != null) {
                o.setShopName(merchant.getShopName());
            }
            User buyer = userMapper.selectById(o.getBuyerId());
            if (buyer != null) {
                o.setBuyerName(buyer.getName());
            }
            Long reviewed = reviewMapper.selectCount(new LambdaQueryWrapper<Review>()
                    .eq(Review::getOrderId, o.getId())
                    .eq(Review::getType, "PRODUCT"));
            o.setReviewed(reviewed != null && reviewed > 0);
        }
    }

    private void addTxn(Long userId, String type, BigDecimal amount, BigDecimal balanceAfter,
                        String remark, Long orderId) {
        WalletTxn txn = new WalletTxn();
        txn.setUserId(userId);
        txn.setType(type);
        txn.setAmount(amount);
        txn.setBalanceAfter(balanceAfter);
        txn.setRemark(remark);
        txn.setOrderId(orderId);
        txn.setCreatedAt(LocalDateTime.now());
        walletTxnMapper.insert(txn);
    }

    private String generateNo() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }
}
