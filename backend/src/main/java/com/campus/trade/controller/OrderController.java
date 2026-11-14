package com.campus.trade.controller;

import com.campus.trade.common.Result;
import com.campus.trade.common.UserContext;
import com.campus.trade.entity.Order;
import com.campus.trade.service.OrderService;
import com.campus.trade.service.ReviewService;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final ReviewService reviewService;

    public OrderController(OrderService orderService, ReviewService reviewService) {
        this.orderService = orderService;
        this.reviewService = reviewService;
    }

    /**
     * 结算下单：支持购物车多选，也支持商品详情页直接购买
     */
    @PostMapping("/checkout")
    public Result<List<Order>> checkout(@RequestBody Map<String, Object> body) {
        List<Long> cartIds = new ArrayList<>();
        Object raw = body.get("cartItemIds");
        if (raw instanceof List) {
            for (Object o : (List<?>) raw) {
                cartIds.add(Long.valueOf(String.valueOf(o)));
            }
        }
        Long productId = body.get("productId") == null ? null : Long.valueOf(String.valueOf(body.get("productId")));
        Integer quantity = body.get("quantity") == null ? 1 : Integer.valueOf(String.valueOf(body.get("quantity")));
        Integer usePoints = body.get("usePoints") == null ? 0 : Integer.valueOf(String.valueOf(body.get("usePoints")));
        return Result.ok(orderService.checkout(UserContext.userId(), cartIds, productId, quantity, usePoints));
    }

    @GetMapping
    public Result<List<Order>> list(@RequestParam(required = false) String status) {
        return Result.ok(orderService.buyerOrders(UserContext.userId(), status));
    }

    @PostMapping("/{id}/confirm")
    public Result<Void> confirm(@PathVariable Long id) {
        orderService.confirm(UserContext.userId(), id);
        return Result.ok();
    }

    @PostMapping("/{id}/return")
    public Result<Void> requestReturn(@PathVariable Long id, @RequestBody Map<String, String> body) {
        orderService.requestReturn(UserContext.userId(), id, body.get("reason"));
        return Result.ok();
    }

    /**
     * 买家评价：商品 + 商家服务态度
     */
    @PostMapping("/{id}/review")
    public Result<Void> review(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Integer productRating = Integer.valueOf(String.valueOf(body.getOrDefault("productRating", 5)));
        Integer merchantRating = body.get("merchantRating") == null ? null
                : Integer.valueOf(String.valueOf(body.get("merchantRating")));
        reviewService.reviewByBuyer(UserContext.userId(), id, productRating,
                (String) body.get("productContent"), merchantRating, (String) body.get("merchantContent"));
        return Result.ok();
    }
}
