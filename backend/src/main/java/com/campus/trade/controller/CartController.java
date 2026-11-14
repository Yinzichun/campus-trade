package com.campus.trade.controller;

import com.campus.trade.common.Result;
import com.campus.trade.common.UserContext;
import com.campus.trade.entity.CartItem;
import com.campus.trade.service.CartService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public Result<List<CartItem>> list() {
        return Result.ok(cartService.list(UserContext.userId()));
    }

    @PostMapping
    public Result<Void> add(@RequestBody Map<String, Object> body) {
        Long productId = Long.valueOf(String.valueOf(body.get("productId")));
        Integer quantity = body.get("quantity") == null ? 1 : Integer.valueOf(String.valueOf(body.get("quantity")));
        cartService.add(UserContext.userId(), productId, quantity);
        return Result.ok();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        cartService.updateQuantity(UserContext.userId(), id, Integer.valueOf(String.valueOf(body.get("quantity"))));
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        cartService.remove(UserContext.userId(), id);
        return Result.ok();
    }

    @DeleteMapping
    public Result<Void> clear() {
        cartService.clear(UserContext.userId());
        return Result.ok();
    }
}
