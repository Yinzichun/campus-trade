package com.campus.trade.controller;

import com.campus.trade.common.Result;
import com.campus.trade.common.UserContext;
import com.campus.trade.entity.Product;
import com.campus.trade.entity.User;
import com.campus.trade.service.AdminService;
import com.campus.trade.service.ProductService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final ProductService productService;

    public AdminController(AdminService adminService, ProductService productService) {
        this.adminService = adminService;
        this.productService = productService;
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        UserContext.requireRole("ADMIN");
        return Result.ok(adminService.stats());
    }

    @GetMapping("/users")
    public Result<List<User>> users(@RequestParam(required = false) String role,
                                    @RequestParam(required = false) String status,
                                    @RequestParam(required = false) String keyword) {
        UserContext.requireRole("ADMIN");
        return Result.ok(adminService.users(role, status, keyword));
    }

    @PostMapping("/users/{id}/audit")
    public Result<Void> audit(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        UserContext.requireRole("ADMIN");
        adminService.auditUser(id, Boolean.parseBoolean(String.valueOf(body.get("pass"))));
        return Result.ok();
    }

    @PutMapping("/users")
    public Result<Void> update(@RequestBody User user) {
        UserContext.requireRole("ADMIN");
        adminService.updateUser(user);
        return Result.ok();
    }

    @DeleteMapping("/users/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        UserContext.requireRole("ADMIN");
        adminService.deleteUser(id);
        return Result.ok();
    }

    @PostMapping("/users/{id}/recharge")
    public Result<Void> recharge(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        UserContext.requireRole("ADMIN");
        adminService.recharge(id, new BigDecimal(String.valueOf(body.get("amount"))));
        return Result.ok();
    }

    @PutMapping("/users/{id}/level")
    public Result<Void> level(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        UserContext.requireRole("ADMIN");
        adminService.setLevel(id, Integer.valueOf(String.valueOf(body.get("level"))));
        return Result.ok();
    }

    @PostMapping("/users/{id}/ban")
    public Result<Void> ban(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        UserContext.requireRole("ADMIN");
        adminService.ban(id, Integer.valueOf(String.valueOf(body.getOrDefault("days", 7))));
        return Result.ok();
    }

    @GetMapping("/products")
    public Result<List<Product>> products(@RequestParam(required = false) String status) {
        UserContext.requireRole("ADMIN");
        return Result.ok(adminService.products(status));
    }

    @PostMapping("/products/{id}/audit")
    public Result<Void> auditProduct(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        UserContext.requireRole("ADMIN");
        productService.audit(id, Boolean.parseBoolean(String.valueOf(body.get("pass"))));
        return Result.ok();
    }
}
