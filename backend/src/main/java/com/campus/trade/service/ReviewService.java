package com.campus.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.trade.common.BizException;
import com.campus.trade.entity.Order;
import com.campus.trade.entity.Product;
import com.campus.trade.entity.Review;
import com.campus.trade.entity.User;
import com.campus.trade.mapper.OrderMapper;
import com.campus.trade.mapper.ProductMapper;
import com.campus.trade.mapper.ReviewMapper;
import com.campus.trade.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReviewService {

    private final ReviewMapper reviewMapper;
    private final OrderMapper orderMapper;
    private final ProductMapper productMapper;
    private final UserMapper userMapper;

    public ReviewService(ReviewMapper reviewMapper, OrderMapper orderMapper,
                         ProductMapper productMapper, UserMapper userMapper) {
        this.reviewMapper = reviewMapper;
        this.orderMapper = orderMapper;
        this.productMapper = productMapper;
        this.userMapper = userMapper;
    }

    /**
     * 买家评价：一次提交商品评价和商家服务态度评价
     */
    @Transactional(rollbackFor = Exception.class)
    public void reviewByBuyer(Long buyerId, Long orderId, Integer productRating, String productContent,
                              Integer merchantRating, String merchantContent) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getBuyerId().equals(buyerId)) {
            throw new BizException("订单不存在");
        }
        if (!"COMPLETED".equals(order.getStatus())) {
            throw new BizException("只有已完成的订单可以评价");
        }
        Long exists = reviewMapper.selectCount(new LambdaQueryWrapper<Review>()
                .eq(Review::getOrderId, orderId)
                .eq(Review::getType, "PRODUCT"));
        if (exists != null && exists > 0) {
            throw new BizException("该订单已经评价过了");
        }
        if (productRating == null || productRating < 1 || productRating > 5) {
            throw new BizException("请给出 1-5 星评价");
        }
        Review r1 = new Review();
        r1.setOrderId(orderId);
        r1.setProductId(order.getProductId());
        r1.setMerchantId(order.getMerchantId());
        r1.setBuyerId(buyerId);
        r1.setType("PRODUCT");
        r1.setRating(productRating);
        r1.setContent(productContent);
        r1.setCreatedAt(LocalDateTime.now());
        reviewMapper.insert(r1);

        if (merchantRating != null && merchantRating >= 1) {
            Review r2 = new Review();
            r2.setOrderId(orderId);
            r2.setProductId(order.getProductId());
            r2.setMerchantId(order.getMerchantId());
            r2.setBuyerId(buyerId);
            r2.setType("MERCHANT");
            r2.setRating(merchantRating);
            r2.setContent(merchantContent);
            r2.setCreatedAt(LocalDateTime.now());
            reviewMapper.insert(r2);
        }

        Product product = productMapper.selectById(order.getProductId());
        if (product != null) {
            int count = product.getRatingCount() == null ? 0 : product.getRatingCount();
            BigDecimal total = product.getRatingAvg().multiply(new BigDecimal(count))
                    .add(new BigDecimal(productRating));
            product.setRatingCount(count + 1);
            product.setRatingAvg(total.divide(new BigDecimal(count + 1), 2, RoundingMode.HALF_UP));
            productMapper.updateById(product);
        }
    }

    /**
     * 商家评价买家，转换为买家好评率
     */
    @Transactional(rollbackFor = Exception.class)
    public void reviewByMerchant(Long merchantId, Long orderId, Integer rating, String content) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getMerchantId().equals(merchantId)) {
            throw new BizException("订单不存在");
        }
        if (!"COMPLETED".equals(order.getStatus())) {
            throw new BizException("只有已完成的订单可以评价买家");
        }
        Long exists = reviewMapper.selectCount(new LambdaQueryWrapper<Review>()
                .eq(Review::getOrderId, orderId)
                .eq(Review::getType, "BUYER"));
        if (exists != null && exists > 0) {
            throw new BizException("已经评价过该买家");
        }
        Review r = new Review();
        r.setOrderId(orderId);
        r.setProductId(order.getProductId());
        r.setMerchantId(merchantId);
        r.setBuyerId(order.getBuyerId());
        r.setType("BUYER");
        r.setRating(rating);
        r.setContent(content);
        r.setCreatedAt(LocalDateTime.now());
        reviewMapper.insert(r);

        List<Review> all = reviewMapper.selectList(new LambdaQueryWrapper<Review>()
                .eq(Review::getBuyerId, order.getBuyerId())
                .eq(Review::getType, "BUYER"));
        double avg = all.stream().mapToInt(Review::getRating).average().orElse(5);
        User buyer = userMapper.selectById(order.getBuyerId());
        buyer.setCreditScore(new BigDecimal(avg * 20).setScale(2, RoundingMode.HALF_UP));
        userMapper.updateById(buyer);
    }

    public List<Review> productReviews(Long productId) {
        List<Review> list = reviewMapper.selectList(new LambdaQueryWrapper<Review>()
                .eq(Review::getProductId, productId)
                .eq(Review::getType, "PRODUCT")
                .orderByDesc(Review::getCreatedAt));
        list.forEach(r -> {
            User u = userMapper.selectById(r.getBuyerId());
            if (u != null) {
                r.setBuyerName(maskName(u.getName()));
            }
        });
        return list;
    }

    public List<Review> merchantReviews(Long merchantId) {
        List<Review> list = reviewMapper.selectList(new LambdaQueryWrapper<Review>()
                .eq(Review::getMerchantId, merchantId)
                .in(Review::getType, java.util.Arrays.asList("PRODUCT", "MERCHANT"))
                .orderByDesc(Review::getCreatedAt));
        list.forEach(r -> {
            User u = userMapper.selectById(r.getBuyerId());
            if (u != null) {
                r.setBuyerName(maskName(u.getName()));
            }
            Order o = orderMapper.selectById(r.getOrderId());
            if (o != null) {
                r.setProductName(o.getProductName());
            }
        });
        return list;
    }

    private String maskName(String name) {
        if (name == null || name.length() <= 1) {
            return name;
        }
        return name.charAt(0) + "**";
    }
}
