package com.campus.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.trade.common.BizException;
import com.campus.trade.entity.CartItem;
import com.campus.trade.entity.Product;
import com.campus.trade.mapper.CartItemMapper;
import com.campus.trade.mapper.ProductMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class CartService {

    private final CartItemMapper cartItemMapper;
    private final ProductMapper productMapper;
    private final ProductService productService;

    public CartService(CartItemMapper cartItemMapper, ProductMapper productMapper, ProductService productService) {
        this.cartItemMapper = cartItemMapper;
        this.productMapper = productMapper;
        this.productService = productService;
    }

    public List<CartItem> list(Long userId) {
        List<CartItem> items = cartItemMapper.selectList(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .orderByDesc(CartItem::getCreatedAt));
        if (items.isEmpty()) {
            return items;
        }
        List<Long> ids = new ArrayList<>();
        items.forEach(i -> ids.add(i.getProductId()));
        List<Product> products = productMapper.selectBatchIds(ids);
        productService.enrich(products);
        for (CartItem item : items) {
            products.stream().filter(p -> p.getId().equals(item.getProductId())).findFirst().ifPresent(p -> {
                item.setProduct(p);
                item.setSubtotal(p.getPrice().multiply(new BigDecimal(item.getQuantity())));
            });
        }
        return items;
    }

    public void add(Long userId, Long productId, Integer quantity) {
        Product product = productMapper.selectById(productId);
        if (product == null || !"ON_SALE".equals(product.getStatus())) {
            throw new BizException("商品已下架或售出");
        }
        int qty = quantity == null || quantity < 1 ? 1 : quantity;
        CartItem exist = cartItemMapper.selectOne(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getProductId, productId));
        if (exist != null) {
            exist.setQuantity(exist.getQuantity() + qty);
            if (exist.getQuantity() > product.getStock()) {
                throw new BizException("超过库存数量");
            }
            cartItemMapper.updateById(exist);
            return;
        }
        if (qty > product.getStock()) {
            throw new BizException("超过库存数量");
        }
        CartItem item = new CartItem();
        item.setUserId(userId);
        item.setProductId(productId);
        item.setQuantity(qty);
        item.setCreatedAt(LocalDateTime.now());
        cartItemMapper.insert(item);
    }

    public void updateQuantity(Long userId, Long id, Integer quantity) {
        CartItem item = cartItemMapper.selectById(id);
        if (item == null || !item.getUserId().equals(userId)) {
            throw new BizException("购物车记录不存在");
        }
        if (quantity == null || quantity < 1) {
            throw new BizException("数量至少为 1");
        }
        item.setQuantity(quantity);
        cartItemMapper.updateById(item);
    }

    public void remove(Long userId, Long id) {
        CartItem item = cartItemMapper.selectById(id);
        if (item == null || !item.getUserId().equals(userId)) {
            throw new BizException("购物车记录不存在");
        }
        cartItemMapper.deleteById(id);
    }

    public void clear(Long userId) {
        cartItemMapper.delete(new LambdaQueryWrapper<CartItem>().eq(CartItem::getUserId, userId));
    }
}
