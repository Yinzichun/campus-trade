package com.campus.trade.controller;

import com.campus.trade.common.BizException;
import com.campus.trade.common.Result;
import com.campus.trade.common.UserContext;
import com.campus.trade.dto.ProductDTO;
import com.campus.trade.entity.Order;
import com.campus.trade.entity.Product;
import com.campus.trade.entity.Review;
import com.campus.trade.entity.User;
import com.campus.trade.service.OrderService;
import com.campus.trade.service.ProductService;
import com.campus.trade.service.ReviewService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/merchant")
public class MerchantController {

    private final ProductService productService;
    private final OrderService orderService;
    private final ReviewService reviewService;

    public MerchantController(ProductService productService, OrderService orderService,
                              ReviewService reviewService) {
        this.productService = productService;
        this.orderService = orderService;
        this.reviewService = reviewService;
    }

    /**
     * 店铺主页，游客也能看
     */
    @GetMapping("/store/{id}")
    public Result<Map<String, Object>> store(@PathVariable Long id) {
        return Result.ok(productService.store(id));
    }

    @GetMapping("/products")
    public Result<List<Product>> myProducts() {
        UserContext.requireRole("MERCHANT");
        return Result.ok(productService.merchantProducts(UserContext.userId()));
    }

    @PostMapping("/products")
    public Result<Product> create(@RequestBody ProductDTO dto) {
        UserContext.requireRole("MERCHANT");
        return Result.ok(productService.create(dto));
    }

    @PutMapping("/products")
    public Result<Product> update(@RequestBody ProductDTO dto) {
        UserContext.requireRole("MERCHANT");
        return Result.ok(productService.update(dto));
    }

    @PutMapping("/products/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        UserContext.requireRole("MERCHANT");
        productService.changeStatus(id, body.get("status"));
        return Result.ok();
    }

    @GetMapping("/orders")
    public Result<List<Order>> orders(@RequestParam(required = false) String status) {
        UserContext.requireRole("MERCHANT");
        return Result.ok(orderService.merchantOrders(UserContext.userId(), status));
    }

    @PostMapping("/orders/{id}/return")
    public Result<Void> handleReturn(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        UserContext.requireRole("MERCHANT");
        boolean approve = Boolean.parseBoolean(String.valueOf(body.get("approve")));
        orderService.handleReturn(UserContext.userId(), id, approve);
        return Result.ok();
    }

    @PostMapping("/orders/{id}/review")
    public Result<Void> reviewBuyer(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        UserContext.requireRole("MERCHANT");
        reviewService.reviewByMerchant(UserContext.userId(), id,
                Integer.valueOf(String.valueOf(body.getOrDefault("rating", 5))),
                (String) body.get("content"));
        return Result.ok();
    }

    @GetMapping("/reviews")
    public Result<List<Review>> reviews() {
        UserContext.requireRole("MERCHANT");
        return Result.ok(reviewService.merchantReviews(UserContext.userId()));
    }

    /**
     * 商家中心首页数据
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        UserContext.requireRole("MERCHANT");
        Long id = UserContext.userId();
        List<Product> products = productService.merchantProducts(id);
        List<Order> orders = orderService.merchantOrders(id, null);
        BigDecimal totalIncome = orders.stream()
                .filter(o -> "COMPLETED".equals(o.getStatus()))
                .map(o -> o.getAmount().subtract(o.getFee()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long onSale = products.stream().filter(p -> "ON_SALE".equals(p.getStatus())).count();
        Map<String, Object> data = new HashMap<>();
        data.put("productCount", products.size());
        data.put("onSaleCount", onSale);
        data.put("orderCount", orders.size());
        data.put("totalIncome", totalIncome);
        data.put("pendingReturn", orders.stream().filter(o -> "RETURN_REQUESTED".equals(o.getStatus())).count());
        return Result.ok(data);
    }
}
